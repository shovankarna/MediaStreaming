package com.astromediavault.AstroMediaVault.consumer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageResolutionGenerationConsumer {

    private final MediaRepository mediaRepository;

    @Value("${storage.local.path}")
    private String localStoragePath;

    private static final Logger logger = LoggerFactory.getLogger(ImageResolutionGenerationConsumer.class);

    private static final int[] TARGET_WIDTHS = { 150, 480, 720, 1080 };
    private static final String[] RESOLUTION_NAMES = { "thumb", "480p", "720p", "1080p" };

    @RabbitListener(queues = "image-resolution-generation-queue")
    public void generateResolutions(String mediaIdStr) {
        try {
            UUID mediaId = UUID.fromString(mediaIdStr);
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));

            String originalPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
            BufferedImage originalImage = ImageIO.read(new File(originalPath));
            if (originalImage == null)
                throw new RuntimeException("Unable to read original image");

            for (int i = 0; i < TARGET_WIDTHS.length; i++) {
                int targetWidth = TARGET_WIDTHS[i];
                String resolutionName = RESOLUTION_NAMES[i];

                String processedRelativePath = Paths.get(
                        "users",
                        media.getUser().getId().toString(),
                        "images",
                        "processed",
                        media.getId().toString(),
                        resolutionName + ".webp").toString();

                Path fullProcessedPath = Paths.get(localStoragePath, processedRelativePath);

                // Skip if already exists
                if (Files.exists(fullProcessedPath)) {
                    logger.info("Skipping existing resolution: {}", fullProcessedPath);
                    continue;
                }

                Files.createDirectories(fullProcessedPath.getParent());

                BufferedImage resized = resizeImage(originalImage, targetWidth);

                // Save as temporary PNG file first
                File tempPng = File.createTempFile("resized-" + resolutionName, ".png");
                ImageIO.write(resized, "png", tempPng);

                // Use cwebp CLI to convert PNG to WebP
                String[] command = {
                    "cwebp",
                    "-q", "85",
                    tempPng.getAbsolutePath(),
                    "-o",
                    fullProcessedPath.toString()
                };
                

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    logger.info("Generated image resolution {} for media {}", resolutionName, mediaId);
                } else {
                    logger.error("Failed to generate {} resolution for media {}", resolutionName, mediaId);
                }

                // Clean up temp file
                if (!tempPng.delete()) {
                    logger.warn("Failed to delete temp file: {}", tempPng.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            logger.error("Failed to generate image resolutions: {}", e.getMessage(), e);
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double aspectRatio = (double) originalHeight / originalWidth;
        int targetHeight = (int) (targetWidth * aspectRatio);

        Image scaledImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();
        return resizedImage;
    }
}
