package com.astromediavault.AstroMediaVault.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "media")
public class Media {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType fileType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private String storagePath;

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subtitle> subtitles = new ArrayList<>();

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TranscodedVideo> transcodedVideos = new ArrayList<>();

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoSegment> videoSegments = new ArrayList<>();

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaybackHistory> playbackHistories = new ArrayList<>();

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MediaTag> mediaTags = new ArrayList<>();

    @OneToOne(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private VideoMetadata videoMetadata;

    @OneToOne(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private ImageMetadata imageMetadata;

    @OneToOne(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    private PdfMetadata pdfMetadata;

    @Column(nullable = false, updatable = false)
    private Instant uploadTimestamp = Instant.now();

    public enum FileType {
        VIDEO, IMAGE, PDF
    }
}
