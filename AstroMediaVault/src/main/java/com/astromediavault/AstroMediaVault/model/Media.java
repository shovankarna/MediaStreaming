package com.astromediavault.AstroMediaVault.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @Column(nullable = false)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType fileType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private String storagePath;

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Subtitle> subtitles;

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TranscodedVideo> transcodedVideos;

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<VideoSegment> videoSegments;

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PlaybackHistory> playbackHistories;

    @OneToMany(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<MediaTag> mediaTags;

    @OneToOne(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private VideoMetadata videoMetadata;

    @OneToOne(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private ImageMetadata imageMetadata;

    @OneToOne(mappedBy = "media", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private PdfMetadata pdfMetadata;
    @Column(nullable = false, updatable = false)
    private Instant uploadTimestamp = Instant.now();

    public enum FileType {
        VIDEO, IMAGE, PDF
    }
}
