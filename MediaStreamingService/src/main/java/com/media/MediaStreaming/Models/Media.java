package com.media.MediaStreaming.Models;

import java.time.LocalDateTime;

import lombok.Data;

import jakarta.persistence.*;

@Entity
@Table(name = "media", indexes = {
        @Index(name = "idx_external_user_id", columnList = "externalUserId"),
        @Index(name = "idx_media_type", columnList = "mediaType")
})
@Data
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    @Column(nullable = false)
    private String externalUserId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    @Column(nullable = false)
    private String filePath;

    private String thumbnailPath;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime uploadDate = LocalDateTime.now();

    @Column(nullable = false)
    private boolean isPublic = false;

    private String secureToken;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MediaStatus status = MediaStatus.UPLOADED; // Default status is 'UPLOADED'
}