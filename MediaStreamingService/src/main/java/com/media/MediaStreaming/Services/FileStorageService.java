package com.media.MediaStreaming.Services;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.media.MediaStreaming.Models.ImageDetails;
import com.media.MediaStreaming.Models.Media;
import com.media.MediaStreaming.Models.MediaType;
import com.media.MediaStreaming.Models.PdfDetails;
import com.media.MediaStreaming.Models.VideoDetails;
import com.media.MediaStreaming.Models.VideoSegment;
import com.media.MediaStreaming.Repository.ImageDetailsRepository;
import com.media.MediaStreaming.Repository.MediaRepository;
import com.media.MediaStreaming.Repository.PdfDetailsRepository;
import com.media.MediaStreaming.Repository.VideoDetailsRepository;

import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired
    private VideoDetailsRepository videoDetailsRepository;

    @Autowired
    private ImageDetailsRepository imageDetailsRepository;

    @Autowired
    private PdfDetailsRepository pdfDetailsRepository;

    public String storeFile(MultipartFile file, String externalUserId) throws Exception {
        logger.info("Starting file upload for user: {}", externalUserId);
        String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
        if (sanitizedFileName.isEmpty()) {
            logger.error("Invalid file name: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("Invalid file name");
        }

        MediaType mediaType = determineMediaType(sanitizedFileName)
                .orElseThrow(() -> {
                    logger.error("Unsupported media type for file: {}", sanitizedFileName);
                    return new IllegalArgumentException("Unsupported media type");
                });

        Path storagePath;
        switch (mediaType) {
            case VIDEO:
                storagePath = Paths.get("/media/videos/");
                break;
            case IMAGE:
                storagePath = Paths.get("/media/images/");
                break;
            case PDF:
                storagePath = Paths.get("/media/documents/");
                break;
            default:
                logger.error("Unhandled media type: {}", mediaType);
                throw new IllegalArgumentException("Unhandled media type");
        }

        try {
            Files.createDirectories(storagePath);
            Path filePath = storagePath.resolve(sanitizedFileName);
            try (FileOutputStream fos = new FileOutputStream(new File(filePath.toString()))) {
                fos.write(file.getBytes());
            }
            logger.info("File saved successfully at path: {}", filePath);

            Media media = new Media();
            media.setExternalUserId(externalUserId);
            media.setMediaType(mediaType);
            media.setFilePath(filePath.toString());
            Media savedMedia = mediaRepository.save(media);
            logger.info("Media details saved to database with ID: {}", savedMedia.getMediaId());

            switch (mediaType) {
                case VIDEO:
                    // Process video after saving it
                    videoProcessingService.processVideo(filePath.toString(), "/media/processed_videos/");
                    // Handle segments and save video details
                    List<VideoSegment> segments = videoProcessingService.getSegments();
                    for (VideoSegment segment : segments) {
                        VideoDetails videoDetails = new VideoDetails();
                        videoDetails.setMedia(savedMedia);
                        videoDetails.setResolution(segment.getResolution());
                        videoDetails.setSegmentPath(segment.getPath());
                        videoDetails.setDuration(segment.getDuration());
                        videoDetails.setCodec(segment.getCodec());
                        videoDetailsRepository.save(videoDetails);
                    }
                    logger.info("Video details processed and saved for file: {}", sanitizedFileName);
                    break;
                case IMAGE:
                    BufferedImage image = ImageIO.read(filePath.toFile());
                    if (image != null) {
                        ImageDetails imageDetails = new ImageDetails();
                        imageDetails.setMedia(savedMedia);
                        imageDetails.setWidth(image.getWidth());
                        imageDetails.setHeight(image.getHeight());
                        imageDetails.setFormat(getImageFormat(sanitizedFileName));
                        imageDetailsRepository.save(imageDetails);
                        logger.info("Image details saved for file: {}", sanitizedFileName);
                    } else {
                        logger.error("Failed to read image file: {}", sanitizedFileName);
                        throw new IOException("Failed to read image file");
                    }
                    break;
                case PDF:
                    try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
                        PdfDetails pdfDetails = new PdfDetails();
                        pdfDetails.setMedia(savedMedia);
                        pdfDetails.setPageCount(document.getNumberOfPages());
                        pdfDetails.setSizeInKb((int) (Files.size(filePath) / 1024));
                        pdfDetailsRepository.save(pdfDetails);
                        logger.info("PDF details saved for file: {}", sanitizedFileName);
                    } catch (IOException e) {
                        logger.error("Failed to read PDF file: {}", sanitizedFileName, e);
                        throw new IOException("Failed to read PDF file", e);
                    }
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            logger.error("Error occurred while storing file: {}", sanitizedFileName, e);
            throw e;
        }

        return "File uploaded and stored successfully!";
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
            case "png":
            case "jpeg":
                return Optional.of(MediaType.IMAGE);
            case "pdf":
                return Optional.of(MediaType.PDF);
            default:
                return Optional.of(MediaType.OTHER);
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
