package com.media.MediaStreaming.Services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.media.MediaStreaming.DTO.VideoProcessingRequest;
import com.media.MediaStreaming.config.RabbitMQConfig;

@Service
public class VideoProcessingProducer {

    private final RabbitTemplate rabbitTemplate;

    public VideoProcessingProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendVideoProcessingTask(VideoProcessingRequest request) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.VIDEO_PROCESSING_QUEUE, request);
    }
}
