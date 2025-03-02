package com.astromediavault.AstroMediaVault.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "video_metadata")
public class VideoMetadata {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "media_id", nullable = false, unique = true)
    private Media media;

    @Column(nullable = false)
    private String title;

    private String description;

    private int durationSeconds;

    private String frameRate;

    private String resolution;

    private String codec;

    private int bitrate;
}