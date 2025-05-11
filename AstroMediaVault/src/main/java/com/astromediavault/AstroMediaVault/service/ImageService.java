package com.astromediavault.AstroMediaVault.service;

import lombok.RequiredArgsConstructor;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.UUID;

import com.astromediavault.AstroMediaVault.dto.MediaUploadRequest;
import com.astromediavault.AstroMediaVault.exception.InvalidFileTypeException;
import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.ImageMetadata;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.repository.ImageMetadataRepository;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageMetadataRepository imageMetadataRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MediaRepository mediaRepository;

    @Value("${storage.local.path}")
    private String localStoragePath;

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_WIDTH = 10000;
    private static final int MAX_HEIGHT = 10000;

    public void processImageUpload(Media media, MediaUploadRequest request, String fullPath) throws Exception {
        MultipartFile file = request.getFile();

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileTypeException("Image file size exceeds maximum allowed (10MB)");
        }

        BufferedImage bufferedImage;
        try (InputStream is = file.getInputStream()) {
            bufferedImage = ImageIO.read(is);
            if (bufferedImage == null) {
                throw new InvalidFileTypeException("Invalid image file");
            }
        }

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            throw new InvalidFileTypeException("Image dimensions exceed allowed limits");
        }

        BufferedImage sanitizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        sanitizedImage.getGraphics().drawImage(bufferedImage, 0, 0, null);

        String extension = getExtension(file);

        ImageIO.write(sanitizedImage, extension, new File(fullPath)); // ✅ Save sanitized image

        media.setStoragePath(Paths
                .get("users", media.getUser().getId().toString(), "images", "original", new File(fullPath).getName())
                .toString());

        ImageMetadata metadata = new ImageMetadata();
        metadata.setMedia(media);
        metadata.setWidth(width);
        metadata.setHeight(height);
        metadata.setColorMode(bufferedImage.getColorModel().toString());
        metadata.setFormat(extension);
        imageMetadataRepository.save(metadata);

        logger.info("Saved original image and metadata for media: {}", media.getId());

        if (request.isGenerateImgRes()) {
            rabbitTemplate.convertAndSend("image-resolution-generation-queue", media.getId().toString());
            logger.info("Queued image for resolution processing: {}", media.getId());
        }
    }

    private String getExtension(MultipartFile file) throws Exception {
        Tika tika = new Tika();
        String mimeType = tika.detect(file.getInputStream());
        return switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new InvalidFileTypeException("Unsupported image format");
        };
    }

    public ResponseEntity<Resource> streamImage(UUID mediaId, String resolution, String format, boolean download) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));

        if (media.getFileType() != Media.FileType.IMAGE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        String filePath;
        if (resolution == null || resolution.isEmpty()) {
            filePath = Paths.get(localStoragePath, media.getStoragePath()).toString();
        } else {
            filePath = Paths.get(
                    localStoragePath,
                    "users",
                    media.getUser().getId().toString(),
                    "images",
                    "processed",
                    media.getId().toString(),
                    resolution + "." + format).toString();
        }

        File file = new File(filePath);
        if (!file.exists()) {
            logger.warn("Requested image file does not exist: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(file.length());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        if (download) {
            headers.setContentDispositionFormData("attachment", file.getName());
        } else {
            headers.setContentDispositionFormData("inline", file.getName());
        }

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    public ResponseEntity<Resource> downloadImage(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        if (media.getFileType() != Media.FileType.IMAGE) {
            throw new InvalidFileTypeException("Requested media is not an image");
        }

        String fullPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
        File file = new File(fullPath);

        if (!file.exists()) {
            throw new RuntimeException("Image file does not exist at: " + fullPath);
        }

        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(file.length());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", file.getName());

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    public void deleteImageFiles(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));

        String originalPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
        File originalFile = new File(originalPath);

        if (originalFile.exists() && originalFile.canWrite()) {
            if (originalFile.delete()) {
                logger.info("Deleted original image file: {}", originalPath);
            } else {
                logger.warn("Failed to delete original image file: {}", originalPath);
            }
        }

        Path processedDir = Paths.get(
                localStoragePath,
                "users",
                media.getUser().getId().toString(),
                "images",
                "processed",
                media.getId().toString());

        if (Files.exists(processedDir)) {
            try {
                Files.walk(processedDir)
                        .map(Path::toFile)
                        .sorted((a, b) -> -a.compareTo(b)) // delete children first
                        .forEach(File::delete);
                logger.info("Deleted processed images for media: {}", mediaId);
            } catch (Exception e) {
                logger.error("Failed to delete processed images: {}", e.getMessage(), e);
            }
        }

        imageMetadataRepository.deleteById(media.getId());
        logger.info("Deleted image metadata for media: {}", mediaId);
    }
}
