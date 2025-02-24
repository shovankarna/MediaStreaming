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

    @Column(nullable = false, updatable = false)
    private Instant uploadTimestamp = Instant.now();

    public enum FileType {
        VIDEO, IMAGE, PDF
    }
}