package com.media.MediaStreaming.Services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.media.MediaStreaming.Models.VideoSegment;

@Service
public class VideoProcessingService {

    private List<VideoSegment> segments = new ArrayList<>();

    public void processVideo(String inputFilePath, String outputDir) throws Exception {
        String[] command = {
                "ffmpeg",
                "-i", inputFilePath,
                "-preset", "fast",
                "-g", "48",
                "-sc_threshold", "0",
                "-map", "0",
                "-c:v", "libx264",
                "-b:v:0", "3000k",
                "-s:v:0", "1920x1080",
                "-b:v:1", "1500k",
                "-s:v:1", "1280x720",
                "-b:v:2", "800k",
                "-s:v:2", "854x480",
                "-hls_time", "4",
                "-hls_list_size", "0",
                "-hls_segment_filename", outputDir + "/%03d.ts",
                outputDir + "/index.m3u8"
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg process failed with exit code " + exitCode);
        }

        // Create segments based on processed video
        for (int i = 0; i < 3; i++) {
            VideoSegment segment = new VideoSegment();
            segment.setResolution(i == 0 ? "1920x1080" : (i == 1 ? "1280x720" : "854x480"));
            segment.setPath(outputDir + "/%03d.ts");
            segment.setDuration(5); // Example duration
            segment.setCodec("h264");
            segments.add(segment);
        }
    }

    public List<VideoSegment> getSegments() {
        return segments;
    }
}
