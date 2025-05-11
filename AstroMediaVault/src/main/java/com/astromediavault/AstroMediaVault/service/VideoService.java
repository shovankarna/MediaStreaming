package com.astromediavault.AstroMediaVault.service;

import com.astromediavault.AstroMediaVault.dto.MediaUploadRequest;
import com.astromediavault.AstroMediaVault.exception.InvalidFileTypeException;
import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.model.TranscodedVideo;
import com.astromediavault.AstroMediaVault.model.VideoSegment;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import com.astromediavault.AstroMediaVault.repository.TranscodedVideoRepository;
import com.astromediavault.AstroMediaVault.repository.VideoMetadataRepository;
import com.astromediavault.AstroMediaVault.repository.VideoSegmentRepository;
import lombok.RequiredArgsConstructor;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final RabbitTemplate rabbitTemplate;
    private final TranscodedVideoRepository transcodedVideoRepository;
    private final VideoSegmentRepository videoSegmentRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final MediaRepository mediaRepository;

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    @Value("${storage.local.path}")
    private String localStoragePath;

    @Value("${server.host}")
    private String serverHost;

    /**
     * Process Video Upload (Send Jobs to RabbitMQ)
     */
    public void processVideoUpload(Media media, MediaUploadRequest request, String fullPath) {
        try (InputStream inputStream = request.getFile().getInputStream()) {
            Files.copy(inputStream, Paths.get(fullPath)); // ✅ Save the video
        } catch (IOException e) {
            throw new RuntimeException("Failed to store video file: " + e.getMessage(), e);
        }

        if (request.getSubtitle() != null && !request.getSubtitle().isEmpty()) {
            logger.info("Subtitle provided with video. Processing...");
            rabbitTemplate.convertAndSend("subtitle-processing-queue", media.getId().toString());
        }

        rabbitTemplate.convertAndSend("video-processing-queue", media.getId().toString());
        rabbitTemplate.convertAndSend("thumbnail-generation-queue", media.getId().toString());

        logger.info("Video processing tasks sent for media: {}", media.getId());
    }

    /**
     * Generate HLS Streaming URL
     */
    public String generateHlsUrl(Media media) {
        String hlsPath = Paths.get("users", media.getUser().getId().toString(), "videos", "hls",
                media.getId().toString(), "master.m3u8").toString();
        return serverHost + "/" + hlsPath.replace("\\", "/");
    }

    /**
     * DOWNLOAD Video File
     */
    public ResponseEntity<Resource> downloadVideo(UUID mediaId) throws IOException {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        if (media.getFileType() != Media.FileType.VIDEO) {
            throw new InvalidFileTypeException("Download is only available for video files.");
        }

        String fullPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
        File file = new File(fullPath);
        if (!file.exists()) {
            throw new MediaNotFoundException("Original video file not found at: " + fullPath);
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + media.getFileName() + "\"")
                .body(resource);
    }

    /**
     * Delete All Video Files and Metadata
     */
    public void deleteVideoFiles(UUID mediaId) {
        logger.info("🔹 Deleting video files for media ID: {}", mediaId);

        // 🔹 Fetch media from the database
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        // 🔹 Fetch and delete video segments
        List<VideoSegment> videoSegments = videoSegmentRepository.findByMediaId(mediaId);
        for (VideoSegment segment : videoSegments) {
            Path segmentPath = Paths.get(localStoragePath, segment.getSegmentPath()).normalize();
            deleteLocalFile(segmentPath);
        }
        videoSegmentRepository.deleteByMediaId(mediaId);

        // 🔹 Fetch and delete transcoded videos
        List<TranscodedVideo> transcodedVideos = transcodedVideoRepository.findByMediaId(mediaId);
        for (TranscodedVideo transcoded : transcodedVideos) {
            Path transcodedPath = Paths.get(localStoragePath, transcoded.getFilePath()).normalize();
            deleteLocalFile(transcodedPath);
        }
        transcodedVideoRepository.deleteByMediaId(mediaId);

        // 🔹 Delete thumbnail (Fixed path)
        Path thumbnailPath = Paths.get(localStoragePath, "users", media.getUser().getId().toString(), "videos",
                "thumbnails", mediaId + ".jpg").normalize();
        deleteLocalFile(thumbnailPath);

        // 🔹 Delete HLS Folder (Fixed path)
        Path hlsFolderPath = Paths
                .get(localStoragePath, "users", media.getUser().getId().toString(), "videos", "hls", mediaId.toString())
                .normalize();
        deleteLocalFolder(hlsFolderPath);

        // 🔹 Delete Original Video (Fixed path)
        Path originalFilePath = Paths.get(localStoragePath, media.getStoragePath()).normalize();
        deleteLocalFile(originalFilePath);

        // 🔹 Delete Video Metadata
        videoMetadataRepository.deleteByMediaId(mediaId);

        logger.info("✅ Successfully deleted all video-related files for media ID: {}", mediaId);
    }

    /**
     * Delete Local File Helper Method
     */
    private void deleteLocalFile(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("✅ Deleted file: {}", filePath);
            } else {
                logger.warn("⚠️ File not found, skipping deletion: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("❌ Failed to delete file: {} - {}", filePath, e.getMessage(), e);
        }
    }

    /**
     * Delete a folder and all its contents
     */
    private void deleteLocalFolder(Path folderPath) {
        try {
            File folder = folderPath.toFile();
            if (folder.exists() && folder.isDirectory()) {
                FileUtils.deleteDirectory(folder);
                logger.info("✅ Deleted folder: {}", folderPath);
            } else {
                logger.warn("⚠️ Folder does not exist, skipping deletion: {}", folderPath);
            }
        } catch (IOException e) {
            logger.error("❌ Failed to delete folder: {} - {}", folderPath, e.getMessage(), e);
        }
    }

}
