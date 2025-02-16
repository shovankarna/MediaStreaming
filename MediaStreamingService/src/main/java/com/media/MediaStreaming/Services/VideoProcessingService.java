package com.media.MediaStreaming.Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.MediaStreaming.DTO.VideoMetadata;
import com.media.MediaStreaming.Models.VideoDetails;
import com.media.MediaStreaming.Models.VideoSegment;
import com.media.MediaStreaming.Repository.VideoDetailsRepository;
import com.media.MediaStreaming.Repository.VideoSegmentRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VideoProcessingService {

    @Autowired
    private VideoDetailsRepository videoDetailsRepository;

    @Autowired
    private VideoSegmentRepository videoSegmentRepository;

    private static final int PROCESS_TIMEOUT_MINUTES = 30;

    public void processVideo(String userId, String inputFilePath, String outputDir, String uniqueVideoId)
            throws Exception {
        log.info("Starting video processing for user: {}, videoId: {}", userId, uniqueVideoId);

        // Normalize file paths
        inputFilePath = new File(inputFilePath).getAbsolutePath();
        outputDir = new File(outputDir).getAbsolutePath();

        // Check FFmpeg installation
        checkFFmpegInstallation();

        // Verify input file exists
        File inputFile = new File(inputFilePath);
        if (!inputFile.exists() || !inputFile.canRead()) {
            throw new RuntimeException("Input file does not exist or is not readable: " + inputFilePath);
        }

        // Ensure output directory exists and is writable
        ensureOutputDirectory(outputDir);

        // Get video metadata
        VideoMetadata metadata = getVideoMetadata(inputFilePath);
        if (metadata == null) {
            throw new RuntimeException("Failed to extract video metadata");
        }

        // Process video with FFmpeg
        executeFFmpegCommand(inputFilePath, outputDir);

        // Verify output was created
        verifyOutputCreated(outputDir);

        // Update database with segment information
        updateDatabaseWithSegments(userId, uniqueVideoId, outputDir, metadata);

        log.info("Video processing completed successfully for videoId: {}", uniqueVideoId);
    }

    private void checkFFmpegInstallation() throws Exception {
        log.debug("Checking FFmpeg installation...");
        try {
            Process checkProcess = Runtime.getRuntime().exec("ffmpeg -version");
            boolean completed = checkProcess.waitFor(1, TimeUnit.MINUTES);

            if (!completed) {
                checkProcess.destroyForcibly();
                throw new RuntimeException("FFmpeg version check timed out");
            }

            int exitCode = checkProcess.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg is not properly installed or accessible");
            }
        } catch (IOException e) {
            log.error("Error checking FFmpeg installation", e);
            throw new RuntimeException("FFmpeg check failed", e);
        }
    }

    private void ensureOutputDirectory(String outputDir) {
        log.debug("Ensuring output directory exists: {}", outputDir);
        File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists()) {
            boolean created = outputDirFile.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create output directory: " + outputDir);
            }
        }
        if (!outputDirFile.canWrite()) {
            throw new RuntimeException("No write permission for output directory: " + outputDir);
        }
    }

    private void executeFFmpegCommand(String inputFilePath, String outputDir) throws Exception {
        String[] command = {
                "ffmpeg", "-i", inputFilePath,
                "-preset", "fast",
                "-keyint_min", "48", "-g", "48", "-sc_threshold", "0",
                "-map", "0:v:0", "-map", "0:a:0",

                // 1080p
                "-filter:v:0", "scale=w=1920:h=1080",
                "-c:v:0", "libx264", "-b:v:0", "5000k",

                // 720p
                "-filter:v:1", "scale=w=1280:h=720",
                "-c:v:1", "libx264", "-b:v:1", "3000k",

                // 480p
                "-filter:v:2", "scale=w=854:h=480",
                "-c:v:2", "libx264", "-b:v:2", "1000k",

                // Audio
                "-c:a", "aac", "-b:a", "128k",

                // HLS Settings
                "-f", "hls",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_flags", "independent_segments",
                "-hls_segment_type", "mpegts",
                "-hls_segment_filename", outputDir + "/%v/segment%d.ts",
                "-master_pl_name", "master.m3u8",
                "-var_stream_map", "v:0,a:0 v:1,a:0 v:2,a:0",
                outputDir + "/%v/playlist.m3u8"
        };

        log.debug("Executing FFmpeg command...");
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Merge error stream with input stream
        Process process = processBuilder.start();

        // Handle process output in separate thread
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            } catch (IOException e) {
                log.error("Error reading FFmpeg output", e);
            }
        });
        outputThread.start();

        // Wait for process to complete with timeout
        boolean completed = process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg process timed out after " + PROCESS_TIMEOUT_MINUTES + " minutes");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg process failed with exit code " + exitCode);
        }
    }

    private void verifyOutputCreated(String outputDir) {
        log.debug("Verifying output files...");
        File masterPlaylist = new File(outputDir, "master.m3u8");
        if (!masterPlaylist.exists()) {
            throw new RuntimeException("Master playlist file was not created");
        }

        String[] resolutions = { "1920x1080", "1280x720", "854x480" };
        for (String resolution : resolutions) {
            File resolutionDir = new File(outputDir, resolution);
            if (!resolutionDir.exists() || resolutionDir.list((dir, name) -> name.endsWith(".ts")).length == 0) {
                throw new RuntimeException("No segments created for resolution: " + resolution);
            }
        }
    }

    private VideoMetadata getVideoMetadata(String inputPath) throws IOException {
        log.debug("Extracting video metadata...");
        String[] command = {
                "ffprobe", "-v", "quiet",
                "-print_format", "json",
                "-show_format", "-show_streams",
                inputPath
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(output.toString(), VideoMetadata.class);
        }
    }

    private void updateDatabaseWithSegments(String userId, String uniqueVideoId,
            String outputDir, VideoMetadata metadata) {
        log.debug("Updating database with segment information...");
        VideoDetails videoDetails = videoDetailsRepository
                .findByMediaExternalUserIdAndUniqueVideoId(userId, uniqueVideoId)
                .orElseThrow(() -> new RuntimeException("Video details not found"));

        // Update video details
        videoDetails.setDuration(metadata.getDuration());
        videoDetails.setCodec(metadata.getVideoCodec());
        videoDetails.setHlsPlaylistPath(outputDir + "/master.m3u8");

        // Create segment entries for each resolution
        List<VideoSegment> segments = new ArrayList<>();
        String[] resolutions = { "1920x1080", "1280x720", "854x480" };

        for (String resolution : resolutions) {
            File segmentDir = new File(outputDir, resolution);
            File[] segmentFiles = segmentDir.listFiles((dir, name) -> name.endsWith(".ts"));

            if (segmentFiles != null) {
                for (File segmentFile : segmentFiles) {
                    VideoSegment segment = new VideoSegment();
                    segment.setVideoDetails(videoDetails);
                    segment.setResolution(resolution);
                    segment.setPath(segmentFile.getAbsolutePath());
                    segment.setDuration(6.0); // segment duration in seconds
                    segment.setCodec("h264");
                    segments.add(segment);
                }
            }
        }

        videoDetails.setVideoSegments(segments);
        videoDetailsRepository.save(videoDetails);
        log.debug("Database update completed successfully");
    }
}