package com.astromediavault.AstroMediaVault.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "image_metadata")
public class ImageMetadata {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "media_id", nullable = false, unique = true)
    private Media media;

    private int width;

    private int height;

    private String colorMode;

    private String format;
}
