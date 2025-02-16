package com.media.MediaStreaming.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.media.MediaStreaming.DTO.VideoProcessingRequest;
import com.media.MediaStreaming.Models.*;
import com.media.MediaStreaming.Repository.*;
import com.media.MediaStreaming.config.RabbitMQConfig;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    // Load paths from application.yml
    @Value("${media.storage.base-path}")
    private String basePath;

    @Value("${media.storage.video-path}")
    private String videoPath;

    @Value("${media.storage.image-path}")
    private String imagePath;

    @Value("${media.storage.pdf-path}")
    private String pdfPath;

    @Value("${media.storage.segment-path}")
    private String segmentPath;

    private final RabbitTemplate rabbitTemplate;
    private final MediaRepository mediaRepository;
    private final VideoDetailsRepository videoDetailsRepository;
    private final ImageDetailsRepository imageDetailsRepository;
    private final PdfDetailsRepository pdfDetailsRepository;

    public FileStorageService(
            RabbitTemplate rabbitTemplate,
            MediaRepository mediaRepository,
            VideoDetailsRepository videoDetailsRepository,
            ImageDetailsRepository imageDetailsRepository,
            PdfDetailsRepository pdfDetailsRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.mediaRepository = mediaRepository;
        this.videoDetailsRepository = videoDetailsRepository;
        this.imageDetailsRepository = imageDetailsRepository;
        this.pdfDetailsRepository = pdfDetailsRepository;
    }

    public String storeFile(MultipartFile file, String externalUserId, boolean keepOriginal) throws IOException {
        logger.info("Starting file upload for user: {}", externalUserId);

        // Sanitize the file name
        String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
        // Determine the media type (video, image, pdf)
        MediaType mediaType = determineMediaType(sanitizedFileName)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported media type"));

        // Get the storage path for the user-specific directory
        Path storagePath = getStoragePath(mediaType, externalUserId);
        Files.createDirectories(storagePath); // Create the directories if they don't exist
        Path filePath = storagePath.resolve(sanitizedFileName); // Resolve the full path for the file

        // Transfer the file to the storage location
        file.transferTo(filePath.toFile());
        logger.info("File saved successfully at: {}", filePath);

        // Create and save media record
        Media media = new Media();
        media.setExternalUserId(externalUserId);
        media.setMediaType(mediaType);
        media.setFilePath(filePath.toString());
        media.setStatus(MediaStatus.UPLOADED); // Set initial status
        Media savedMedia = mediaRepository.save(media);

        // Update status to "PROCESSING"
        savedMedia.setStatus(MediaStatus.PROCESSING);
        mediaRepository.save(savedMedia);

        // Process the file based on its media type
        switch (mediaType) {
            case VIDEO:
                handleVideoFile(filePath, savedMedia, keepOriginal, externalUserId);
                break;
            case IMAGE:
                handleImageFile(filePath, savedMedia);
                break;
            case PDF:
                handlePdfFile(filePath, savedMedia);
                break;
            default:
                logger.warn("Unhandled media type: {}", mediaType);
        }

        return "File uploaded and processed successfully!";
    }

    private Path getStoragePath(MediaType mediaType, String externalUserId) {
        // Construct the user-specific path dynamically
        switch (mediaType) {
            case VIDEO:
                return Paths.get(basePath + "/media/" + externalUserId + videoPath);
            case IMAGE:
                return Paths.get(basePath + "/media/" + externalUserId + imagePath);
            case PDF:
                return Paths.get(basePath + "/media/" + externalUserId + pdfPath);
            default:
                throw new IllegalArgumentException("Unsupported media type");
        }
    }

    private void handleVideoFile(Path filePath, Media savedMedia, boolean keepOriginal, String externalUserId) {
        String uniqueVideoId = UUID.randomUUID().toString();

        // Create user-specific directories
        String originalVideoDir = basePath + "/media/" + externalUserId + videoPath + "/" + uniqueVideoId;
        String segmentedVideoDir = basePath + "/media/" + externalUserId + segmentPath + "/" + uniqueVideoId;

        try {
            Files.createDirectories(Paths.get(originalVideoDir));
            Files.createDirectories(Paths.get(segmentedVideoDir));

            // Move the uploaded file to original videos directory
            Path destinationPath = Paths.get(originalVideoDir, filePath.getFileName().toString());
            Files.move(filePath, destinationPath);

            VideoProcessingRequest request = new VideoProcessingRequest();
            request.setUserId(externalUserId);
            request.setFilePath(destinationPath.toString());
            request.setOutputPath(segmentedVideoDir);
            request.setKeepOriginal(keepOriginal);
            request.setMediaId(savedMedia.getMediaId().toString());
            request.setUniqueVideoId(uniqueVideoId);
            request.setTargetResolutions(Arrays.asList("1920x1080", "1280x720", "854x480"));

            // Save initial video details
            VideoDetails videoDetails = new VideoDetails();
            videoDetails.setMedia(savedMedia);
            videoDetails.setOriginalVideoPath(destinationPath.toString());
            videoDetails.setSegmentedFolderPath(segmentedVideoDir);
            videoDetails.setUniqueVideoId(uniqueVideoId);
            videoDetailsRepository.save(videoDetails);

            rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_PROCESSING_QUEUE, request);
            logger.info("Video processing task queued for file: {}", destinationPath);

        } catch (IOException e) {
            logger.error("Error handling video file", e);
            throw new RuntimeException("Failed to process video file", e);
        }
    }

    private void handleImageFile(Path filePath, Media savedMedia) throws IOException {
        BufferedImage image = ImageIO.read(filePath.toFile());
        if (image != null) {
            ImageDetails imageDetails = new ImageDetails();
            imageDetails.setMedia(savedMedia);
            imageDetails.setWidth(image.getWidth());
            imageDetails.setHeight(image.getHeight());
            imageDetails.setFormat(getImageFormat(filePath.toString()));
            imageDetailsRepository.save(imageDetails);

            savedMedia.setStatus(MediaStatus.PROCESSED);
            mediaRepository.save(savedMedia);
        } else {
            savedMedia.setStatus(MediaStatus.FAILED);
            mediaRepository.save(savedMedia);
            throw new IOException("Failed to read image file");
        }
    }

    private void handlePdfFile(Path filePath, Media savedMedia) throws IOException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PdfDetails pdfDetails = new PdfDetails();
            pdfDetails.setMedia(savedMedia);
            pdfDetails.setPageCount(document.getNumberOfPages());
            pdfDetails.setSizeInKb((int) (Files.size(filePath) / 1024));
            pdfDetailsRepository.save(pdfDetails);

            savedMedia.setStatus(MediaStatus.PROCESSED);
            mediaRepository.save(savedMedia);
        } catch (IOException e) {
            savedMedia.setStatus(MediaStatus.FAILED);
            mediaRepository.save(savedMedia);
            throw e;
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }

    private Optional<MediaType> determineMediaType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "mp4":
            case "avi":
            case "mov":
                return Optional.of(MediaType.VIDEO);
            case "jpg":
            case "jpeg":
            case "png":
                return Optional.of(MediaType.IMAGE);
            case "pdf":
                return Optional.of(MediaType.PDF);
            default:
                return Optional.empty();
        }
    }

    private String getImageFormat(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "JPEG";
            case "png":
                return "PNG";
            default:
                return "UNKNOWN";
        }
    }
}
