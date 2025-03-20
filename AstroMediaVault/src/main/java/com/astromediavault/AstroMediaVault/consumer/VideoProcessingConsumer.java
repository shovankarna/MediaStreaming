package com.astromediavault.AstroMediaVault.consumer;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.model.TranscodedVideo;
import com.astromediavault.AstroMediaVault.model.VideoMetadata;
import com.astromediavault.AstroMediaVault.model.VideoSegment;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import com.astromediavault.AstroMediaVault.repository.TranscodedVideoRepository;
import com.astromediavault.AstroMediaVault.repository.VideoMetadataRepository;
import com.astromediavault.AstroMediaVault.repository.VideoSegmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoProcessingConsumer {

    private final MediaRepository mediaRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final TranscodedVideoRepository transcodedVideoRepository;
    private final VideoSegmentRepository videoSegmentRepository;

    @Value("${storage.local.path}")
    private String localStoragePath;

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingConsumer.class);

    @RabbitListener(queues = "video-processing-queue")
    public void processVideo(String mediaId) {
        UUID id = UUID.fromString(mediaId);
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found: " + id));

        String fullPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
        File originalVideo = new File(fullPath);

        if (!originalVideo.exists()) {
            logger.error("Video file not found for processing: {}", fullPath);
            return;
        }

        // 🔍 Extract metadata from video
        VideoMetadata metadata = extractVideoMetadata(originalVideo, media);
        videoMetadataRepository.save(metadata);

        // 📂 Organize HLS storage path
        String hlsDirectory = Paths.get(localStoragePath, "users", media.getUser().getId().toString(), "videos", "hls",
                media.getId().toString()).toString();
        File hlsFolder = new File(hlsDirectory);
        if (!hlsFolder.exists() && !hlsFolder.mkdirs()) {
            logger.error("Failed to create HLS folder: {}", hlsDirectory);
            return;
        }

        logger.info("Processing video for HLS: {}", originalVideo.getPath());

        try {
            // 🔥 FFmpeg command for HLS generation
            String[] command = {
                    "ffmpeg", "-i", originalVideo.getPath(),
                    "-preset", "veryfast", "-g", "48", "-sc_threshold", "0",

                    "-map", "0:v", "-map", "0:a", "-c:v:0", "libx264", "-b:v:0", "800k", "-s:v:0", "640x360",
                    "-map", "0:v", "-map", "0:a", "-c:v:1", "libx264", "-b:v:1", "1400k", "-s:v:1", "1280x720",
                    "-map", "0:v", "-map", "0:a", "-c:v:2", "libx264", "-b:v:2", "2800k", "-s:v:2", "1920x1080",

                    "-f", "hls",
                    "-hls_time", "6",
                    "-hls_playlist_type", "vod",
                    "-hls_segment_filename", hlsDirectory + "/stream_%v_%03d.ts",
                    "-master_pl_name", "master.m3u8",
                    "-var_stream_map", "v:0,a:0 v:1,a:1 v:2,a:2",
                    hlsDirectory + "/stream_%v.m3u8"
            };

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Video processing completed successfully: {}", media.getId());

                // Store transcoded videos in DB
                saveTranscodedVideo(media, hlsDirectory, metadata);
                saveVideoSegments(media, hlsDirectory);
            } else {
                logger.error("Video processing failed with exit code {}: {}", exitCode, media.getId());
            }
        } catch (Exception e) {
            logger.error("Video processing failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract video metadata using FFmpeg
     */
    private VideoMetadata extractVideoMetadata(File videoFile, Media media) {
        try {
            String[] command = { "ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries",
                    "stream=width,height,codec_name,r_frame_rate,bit_rate,duration", "-of", "json",
                    videoFile.getAbsolutePath() };

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(process.getInputStream());

            JsonNode stream = jsonNode.get("streams").get(0);
            return VideoMetadata.builder()
                    .media(media)
                    .title(media.getFileName())
                    .description("Extracted via FFmpeg")
                    .resolution(stream.get("width").asInt() + "x" + stream.get("height").asInt())
                    .frameRate(stream.get("r_frame_rate").asText())
                    .codec(stream.get("codec_name").asText())
                    .bitrate(stream.get("bit_rate").asInt())
                    .durationSeconds(stream.get("duration").asInt())
                    .build();

        } catch (Exception e) {
            logger.error("Failed to extract video metadata: {}", e.getMessage(), e);
            throw new RuntimeException("Metadata extraction failed");
        }
    }

    /**
     * Save transcoded video details
     */
    private void saveTranscodedVideo(Media media, String hlsDirectory, VideoMetadata metadata) {
        String relativeHlsDirectory = Paths.get("users", media.getUser().getId().toString(), "videos", "hls",
                media.getId().toString()).toString(); // ✅ Store as relative path

        List<String> resolutions = List.of("640x360", "1280x720", "1920x1080");
        List<String> bitrates = List.of("800k", "1400k", "2800k");

        for (int i = 0; i < resolutions.size(); i++) {
            TranscodedVideo transcodedVideo = new TranscodedVideo();
            transcodedVideo.setMedia(media);
            transcodedVideo.setResolution(resolutions.get(i));
            transcodedVideo.setBitrate(Integer.parseInt(bitrates.get(i).replace("k", "000")));
            transcodedVideo.setFilePath(Paths.get(relativeHlsDirectory, "stream_" + i + ".m3u8").toString()); // ✅ Store
                                                                                                              // relative
            transcodedVideoRepository.save(transcodedVideo);
        }
    }

    /**
     * Save video segments in database
     */
    private void saveVideoSegments(Media media, String hlsDirectory) {
        String relativeHlsDirectory = Paths.get("users", media.getUser().getId().toString(), "videos", "hls",
                media.getId().toString()).toString(); // ✅ Store as relative path

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 10; j++) { // Assuming 10 segments per resolution
                VideoSegment segment = new VideoSegment();
                segment.setMedia(media);
                segment.setSegmentIndex(j);
                segment.setResolution(i == 0 ? "360p" : i == 1 ? "720p" : "1080p");
                segment.setSegmentPath(
                        Paths.get(relativeHlsDirectory, "stream_" + i + "_" + String.format("%03d.ts", j)).toString()); // ✅
                                                                                                                        // Store
                                                                                                                        // relative
                videoSegmentRepository.save(segment);
            }
        }
    }

}
