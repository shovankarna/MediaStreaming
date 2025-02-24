package com.astromediavault.AstroMediaVault.controller;

import com.astromediavault.AstroMediaVault.dto.ApiResponse;
import com.astromediavault.AstroMediaVault.dto.MediaResponse;
import com.astromediavault.AstroMediaVault.dto.MediaUploadRequest;
import com.astromediavault.AstroMediaVault.service.MediaService;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);

    /**
     * Upload a media file
     * 
     * @param title       The title of the media
     * @param description The description of the media
     * @param fileType    The type of media (VIDEO, IMAGE, PDF)
     * @param file        The media file
     * @return ResponseEntity containing success message and media path
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadMedia(
            @RequestParam("userId") UUID userId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("fileType") String fileType,
            @RequestParam("file") MultipartFile file) {

        logger.info("Received request to upload media: userId={}, title={}, fileType={}", userId, title, fileType);

        MediaUploadRequest request = new MediaUploadRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setFileType(fileType);
        request.setFile(file);

        ApiResponse<String> response = mediaService.uploadMedia(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Download a media file
     */
    @GetMapping("/download/{mediaId}")
    public ResponseEntity<Resource> downloadMedia(@PathVariable UUID mediaId) {
        return mediaService.downloadMedia(mediaId);
    }

    /**
     * Fetch all uploaded media
     */
    @GetMapping("/fetch-all")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> fetchAllMedia() {
        logger.info("Fetching all media files");
        return ResponseEntity.ok(mediaService.fetchAllMedia());
    }

    /**
     * Fetch media by ID
     */
    @GetMapping("/fetch/{mediaId}")
    public ResponseEntity<ApiResponse<MediaResponse>> fetchMediaById(@PathVariable UUID mediaId) {
        logger.info("Fetching media with ID: {}", mediaId);
        return ResponseEntity.ok(mediaService.fetchMediaById(mediaId));
    }

    /**
     * Fetch media by type (VIDEO, IMAGE, PDF)
     */
    @GetMapping("/fetch/type/{fileType}")
    public ResponseEntity<ApiResponse<List<MediaResponse>>> fetchMediaByType(@PathVariable String fileType) {
        logger.info("Fetching media of type: {}", fileType);
        return ResponseEntity.ok(mediaService.fetchMediaByType(fileType));
    }

    /**
     * Delete media by ID
     */
    @DeleteMapping("/delete/{mediaId}")
    public ResponseEntity<ApiResponse<String>> deleteMedia(@PathVariable UUID mediaId) {
        return ResponseEntity.ok(mediaService.deleteMedia(mediaId));
    }
}
