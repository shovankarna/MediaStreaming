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
@Table(name = "playback_history")
public class PlaybackHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    private int watchedSeconds;

    private boolean completed;

    @Column(nullable = false, updatable = false)
    private Instant lastWatched = Instant.now();
}
