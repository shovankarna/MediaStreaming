package com.media.MediaStreaming.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.media.MediaStreaming.DTO.VideoProcessingRequest;
import com.media.MediaStreaming.Models.Media;
import com.media.MediaStreaming.Models.MediaStatus;
import com.media.MediaStreaming.Repository.MediaRepository;
import com.media.MediaStreaming.config.RabbitMQConfig;

@Service
public class VideoProcessingConsumer {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingConsumer.class);

    private final VideoProcessingService videoProcessingService;
    private final MediaRepository mediaRepository;

    public VideoProcessingConsumer(VideoProcessingService videoProcessingService,
            MediaRepository mediaRepository) {
        this.videoProcessingService = videoProcessingService;
        this.mediaRepository = mediaRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.VIDEO_PROCESSING_QUEUE)
    public void processVideo(VideoProcessingRequest request) {
        logger.info("Received video processing request for media ID: {}", request.getMediaId());

        Media media = mediaRepository.findById(Long.parseLong(request.getMediaId())).orElse(null);

        if (media == null) {
            logger.error("Media not found for ID: {}", request.getMediaId());
            return;
        }

        updateMediaStatus(media, MediaStatus.PROCESSING);

        System.out.println("request.getFilePath() ====>" + request.getFilePath());
        System.out.println("request.getUserId() ====>" + request.getUserId());


        try {
            videoProcessingService.processVideo(request.getUserId(), request.getFilePath(), request.getOutputPath(), request.getUniqueVideoId());
            logger.info("Video processing completed for file: {}", request.getFilePath());

            if (!request.isKeepOriginal()) {
                deleteOriginalFile(request.getFilePath());
            }

            updateMediaStatus(media, MediaStatus.PROCESSED);
        } catch (Exception e) {
            logger.error("Error processing video for media ID: {}", request.getMediaId(), e);
            updateMediaStatus(media, MediaStatus.FAILED);
        }
    }

    private void deleteOriginalFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Original video deleted: {}", filePath);
            }
        } catch (Exception e) {
            logger.error("Failed to delete original video: {}", filePath, e);
        }
    }

    private void updateMediaStatus(Media media, MediaStatus status) {
        try {
            media.setStatus(status);
            mediaRepository.save(media);
        } catch (Exception e) {
            logger.error("Error updating media status for ID: {}", media.getMediaId(), e);
        }
    }
}
