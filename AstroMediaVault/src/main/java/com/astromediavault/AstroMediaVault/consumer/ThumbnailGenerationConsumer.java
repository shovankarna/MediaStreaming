package com.astromediavault.AstroMediaVault.consumer;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThumbnailGenerationConsumer {

    private final MediaRepository mediaRepository;

    @Value("${storage.local.path}")
    private String localStoragePath;

    private static final Logger logger = LoggerFactory.getLogger(ThumbnailGenerationConsumer.class);

    @RabbitListener(queues = "thumbnail-generation-queue")
    public void generateThumbnail(String mediaId) {
        UUID id = UUID.fromString(mediaId);
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found: " + id));

        // Construct full file path dynamically
        String fullPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
        File videoFile = new File(fullPath);

        if (!videoFile.exists()) {
            logger.error("Video file not found for thumbnail generation: {}", fullPath);
            return;
        }

        // 📂 Store thumbnails in a separate directory
        String thumbnailDir = Paths
                .get(localStoragePath, "users", media.getUser().getId().toString(), "videos", "thumbnails").toString();
        File thumbnailFolder = new File(thumbnailDir);
        if (!thumbnailFolder.exists() && !thumbnailFolder.mkdirs()) {
            logger.error("Failed to create thumbnail folder: {}", thumbnailDir);
            return;
        }

        String thumbnailPath = Paths.get(thumbnailDir, media.getId().toString() + ".jpg").toString();

        String[] command = {
                "ffmpeg", "-i", videoFile.getPath(),
                "-ss", "00:00:05", "-vframes", "1",
                thumbnailPath
        };

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();

            logger.info("Thumbnail generated: {}", thumbnailPath);
        } catch (Exception e) {
            logger.error("Thumbnail generation failed: {}", e.getMessage(), e);
        }
    }
}