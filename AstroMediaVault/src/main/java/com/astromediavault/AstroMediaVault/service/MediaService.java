package com.astromediavault.AstroMediaVault.service;

import com.astromediavault.AstroMediaVault.dto.ApiResponse;
import com.astromediavault.AstroMediaVault.dto.MediaResponse;
import com.astromediavault.AstroMediaVault.dto.MediaUploadRequest;
import com.astromediavault.AstroMediaVault.exception.InvalidFileTypeException;
import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.model.User;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import com.astromediavault.AstroMediaVault.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

    @Value("${storage.local.path}")
    private String localStoragePath;

    public ApiResponse<String> uploadMedia(MediaUploadRequest request, UUID userId) {
        try {
            Media.FileType fileType = validateFileType(request.getFileType());
            MultipartFile file = request.getFile();
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new MediaNotFoundException("User not found with ID: " + userId));

            // 📂 Define base user directory
            String userBaseDir = Paths.get("users", userId.toString()).toString();

            // 📂 Create separate folders for different media types (relative paths)
            String mediaFolder = switch (fileType) {
                case VIDEO -> Paths.get(userBaseDir, "videos", "original").toString();
                case IMAGE -> Paths.get(userBaseDir, "images", "original").toString();
                case PDF -> Paths.get(userBaseDir, "pdfs", "original").toString();
            };

            File directory = new File(Paths.get(localStoragePath, mediaFolder).toString());
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Failed to create directory: " + mediaFolder);
            }

            // Save the file using relative path
            String relativePath = Paths.get(mediaFolder, fileName).toString();
            String fullPath = Paths.get(localStoragePath, relativePath).toString();

            File localFile = new File(fullPath);
            file.transferTo(localFile);

            // Save media metadata in the database (only storing relative path)
            Media media = new Media();
            media.setUser(user);
            media.setFileName(file.getOriginalFilename());
            media.setFileSize(file.getSize());
            media.setFileType(fileType);
            media.setStoragePath(relativePath); // Store only relative path
            mediaRepository.save(media);

            // 🔴 Send jobs to RabbitMQ for processing
            if (fileType == Media.FileType.VIDEO) {
                rabbitTemplate.convertAndSend("video-processing-queue", media.getId().toString());
                rabbitTemplate.convertAndSend("thumbnail-generation-queue", media.getId().toString());
            } else if (fileType == Media.FileType.IMAGE) {
                rabbitTemplate.convertAndSend("image-thumbnail-generation-queue", media.getId().toString());
            } else if (fileType == Media.FileType.PDF) {
                rabbitTemplate.convertAndSend("pdf-preview-generation-queue", media.getId().toString());
            }

            logger.info("Media uploaded successfully: {}", fullPath);
            return ApiResponse.success("File uploaded successfully!", relativePath);

        } catch (Exception e) {
            logger.error("Media upload failed: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to upload media.");
        }
    }

    /**
     * Download media file
     */
    public ResponseEntity<Resource> downloadMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        try {
            // Dynamically construct full file path
            String fullPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
            File file = new File(fullPath);
            if (!file.exists()) {
                throw new MediaNotFoundException("Media file not found on disk: " + fullPath);
            }

            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + media.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            throw new RuntimeException("Error downloading media file", e);
        }
    }

    /**
     * Fetch all media
     */
    public ApiResponse<List<MediaResponse>> fetchAllMedia() {
        List<MediaResponse> mediaList = mediaRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        logger.info("Retrieved {} media files", mediaList.size());
        return ApiResponse.success("Media retrieved successfully", mediaList);
    }

    /**
     * Fetch media by ID
     */
    public ApiResponse<MediaResponse> fetchMediaById(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        logger.info("Retrieved media: {}", mediaId);
        return ApiResponse.success("Media retrieved successfully", convertToResponse(media));
    }

    /**
     * Fetch media by type
     */
    public ApiResponse<List<MediaResponse>> fetchMediaByType(String fileType) {
        try {
            Media.FileType type = Media.FileType.valueOf(fileType.toUpperCase());
            List<MediaResponse> mediaList = mediaRepository.findByFileType(type)
                    .stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            logger.info("Retrieved {} media files of type {}", mediaList.size(), fileType);
            return ApiResponse.success("Media retrieved successfully", mediaList);
        } catch (IllegalArgumentException e) {
            throw new InvalidFileTypeException("Invalid file type! Use VIDEO, IMAGE, or PDF.");
        }
    }

    /**
     * Delete media file
     */
    public ApiResponse<String> deleteMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        try {
            // Construct full path dynamically
            String fullPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
            File file = new File(fullPath);
            if (file.exists() && !file.delete()) {
                throw new IOException("Failed to delete file: " + fullPath);
            }

            // Remove from database
            mediaRepository.delete(media);
            logger.info("Deleted media: {}", mediaId);

            return ApiResponse.success("Media deleted successfully!", null);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting media file", e);
        }
    }

    /**
     * Convert Media to MediaResponse DTO
     */
    private MediaResponse convertToResponse(Media media) {
        return MediaResponse.builder()
                .id(media.getId())
                .fileName(media.getFileName())
                .fileType(media.getFileType().toString())
                .fileSize(media.getFileSize())
                .storagePath(media.getStoragePath()) // Returns relative path
                .uploadTimestamp(media.getUploadTimestamp())
                .build();
    }

    /**
     * Validate file type
     */
    private Media.FileType validateFileType(String fileType) {
        try {
            return Media.FileType.valueOf(fileType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidFileTypeException("Invalid file type! Must be VIDEO, IMAGE, or PDF.");
        }
    }

    public ResponseEntity<String> streamVideo(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        String hlsPath = Paths.get("users", media.getUser().getId().toString(), "videos", "hls",
                media.getId().toString(), "master.m3u8").toString();
        String hlsUrl = "http://localhost:8080/" + hlsPath.replace("\\", "/");
        System.out.println("hlsUrl ==>" + hlsUrl);

        return ResponseEntity.ok(hlsUrl);
    }
}
