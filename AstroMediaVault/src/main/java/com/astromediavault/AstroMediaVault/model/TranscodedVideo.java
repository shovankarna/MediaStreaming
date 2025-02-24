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
@Table(name = "transcoded_videos")
public class TranscodedVideo {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(nullable = false)
    private String resolution;

    @Column(nullable = false)
    private String filePath;

    private String codec;

    private int bitrate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
