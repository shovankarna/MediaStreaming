package com.astromediavault.AstroMediaVault.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    /**
     * Creates a queue for video processing tasks.
     * These jobs will transcode videos into multiple resolutions.
     */
    @Bean
    public Queue videoProcessingQueue() {
        return new Queue("video-processing-queue", true);
    }

    /**
     * Creates a queue for thumbnail generation tasks.
     * These jobs will extract a preview image from videos.
     */
    @Bean
    public Queue thumbnailGenerationQueue() {
        return new Queue("thumbnail-generation-queue", true);
    }

    /**
     * Creates a preview for PDF (1st page) generation tasks.
     * These jobs will extract a preview image from pdfs.
     */
    @Bean
    public Queue pdfPreviewGenerationQueue() {
        return new Queue("pdf-preview-generation-queue", true);
    }

    /**
     * Establishes a connection to RabbitMQ.
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory("localhost");
    }

    /**
     * Creates a RabbitTemplate for sending messages to queues.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }
}