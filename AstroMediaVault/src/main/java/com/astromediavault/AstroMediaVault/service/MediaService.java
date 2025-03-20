package com.astromediavault.AstroMediaVault.service;

import com.astromediavault.AstroMediaVault.dto.*;
import com.astromediavault.AstroMediaVault.exception.InvalidFileTypeException;
import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.model.User;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import com.astromediavault.AstroMediaVault.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.core.io.Resource;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final VideoService videoService;
    private final SubtitleService subtitleService;

    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

    @Value("${storage.local.path}")
    private String localStoragePath;

    @Value("${server.host}")
    private String serverHost;

    /**
     * Upload Media (Delegates to respective services)
     */
    public ApiResponse<String> uploadMedia(MediaUploadRequest request, UUID userId) {
        try {
            Media.FileType fileType = validateFileType(request.getFileType());
            MultipartFile file = request.getFile();
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new MediaNotFoundException("User not found with ID: " + userId));

            String userBaseDir = Paths.get("users", userId.toString()).toString();
            String mediaFolder = switch (fileType) {
                case VIDEO -> Paths.get(userBaseDir, "videos", "original").toString();
                case IMAGE -> Paths.get(userBaseDir, "images", "original").toString();
                case PDF -> Paths.get(userBaseDir, "pdfs", "original").toString();
            };

            File directory = new File(Paths.get(localStoragePath, mediaFolder).toString());
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Failed to create directory: " + mediaFolder);
            }

            String relativePath = Paths.get(mediaFolder, fileName).toString();
            String fullPath = Paths.get(localStoragePath, relativePath).toString();
            file.transferTo(new File(fullPath));

            Media media = new Media();
            media.setUser(user);
            media.setFileName(file.getOriginalFilename());
            media.setFileSize(file.getSize());
            media.setFileType(fileType);
            media.setStoragePath(relativePath);
            media = mediaRepository.save(media);

            System.out.println("media.getSubtitles() ===>" + media.getSubtitles());
            System.out.println("request.getSubtitle().isEmpty() ===>" + request.getSubtitle().isEmpty());

            if (fileType == Media.FileType.VIDEO && request.getSubtitle() != null && !request.getSubtitle().isEmpty()) {
                subtitleService.saveSubtitle(request.getSubtitle(), request.getSubtitleLanguage(), media);
            }

            if (fileType == Media.FileType.VIDEO) {
                videoService.processVideoUpload(media, request);
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
     * Download Media
     */
    public ResponseEntity<Resource> downloadMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        try {
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
     * Fetch All Media
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
     * Fetch Media by ID
     */
    public ApiResponse<MediaResponse> fetchMediaById(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        logger.info("Retrieved media: {}", mediaId);
        return ApiResponse.success("Media retrieved successfully", convertToResponse(media));
    }

    /**
     * Fetch Media by Type (VIDEO, IMAGE, PDF)
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
     * Delete Media
     */
    @Transactional
    public ApiResponse<String> deleteMedia(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        try {
            Path basePath = Paths.get(localStoragePath, "users", media.getUser().getId().toString());

            if (media.getFileType() == Media.FileType.VIDEO) {
                videoService.deleteVideoFiles(mediaId);
                subtitleService.deleteSubtitlesForMedia(mediaId);
            } else {
                deleteLocalFile(basePath.resolve(media.getStoragePath()).toString());
            }

            mediaRepository.delete(media);
            logger.info("Deleted media: {}", mediaId);

            return ApiResponse.success("Media deleted successfully!", null);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting media file", e);
        }
    }

    /**
     * Get Video Streaming URL with Subtitles
     */
    public ApiResponse<StreamingResponse> getStreamUrlWithSubtitles(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));

        if (media.getFileType() != Media.FileType.VIDEO) {
            throw new InvalidFileTypeException("Streaming is only available for videos.");
        }

        // ✅ Generate OS-independent HLS URL
        String streamUrl = generateHlsUrl(media);
        List<SubtitleResponse> subtitles = subtitleService.getSubtitlesForMedia(media.getId());

        return ApiResponse.success("Streaming URL generated successfully", new StreamingResponse(streamUrl, subtitles));
    }

    public String generateHlsUrl(Media media) {
        String hlsPath = Paths.get("users", media.getUser().getId().toString(), "videos", "hls",
                media.getId().toString(), "master.m3u8")
                .toString()
                .replace("\\", "/"); // Ensure it works for Windows/Linux

        return serverHost + "/" + hlsPath;
    }

    /**
     * Validate File Type
     */
    private Media.FileType validateFileType(String fileType) {
        try {
            return Media.FileType.valueOf(fileType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidFileTypeException("Invalid file type! Must be VIDEO, IMAGE, or PDF.");
        }
    }

    /**
     * Delete Local File Helper
     */
    private void deleteLocalFile(String filePath) {
        File file = new File(filePath);

        // Debugging logs
        if (!file.exists()) {
            System.out.println("File does not exist, skipping deletion: {}" + filePath);
            logger.warn("File does not exist, skipping deletion: {}", filePath);
            return;
        }

        if (!file.canWrite()) {
            System.out.println("No write permission for file: {}" + filePath);
            logger.error("No write permission for file: {}", filePath);
            return;
        }

        if (file.delete()) {
            System.out.println("Deleted file successfully: {}" + filePath);
            logger.info("Deleted file successfully: {}", filePath);
        } else {
            System.out.println("Failed to delete file: {}" + filePath);
            logger.error("Failed to delete file: {}", filePath);
        }
    }

    /**
     * Convert Media Entity to Response DTO
     */
    private MediaResponse convertToResponse(Media media) {
        List<SubtitleResponse> subtitleResponses = media.getFileType() == Media.FileType.VIDEO
                ? subtitleService.getSubtitlesForMedia(media.getId())
                : new ArrayList<>();

        return MediaResponse.builder()
                .id(media.getId())
                .fileName(media.getFileName())
                .fileType(media.getFileType().toString())
                .fileSize(media.getFileSize())
                .storagePath(media.getStoragePath())
                .uploadTimestamp(media.getUploadTimestamp())
                .subtitles(subtitleResponses)
                .build();
    }
}
