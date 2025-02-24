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
@Table(name = "subtitles")
public class Subtitle {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String subtitlePath;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
