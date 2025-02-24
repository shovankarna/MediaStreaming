package com.astromediavault.AstroMediaVault.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pdf_metadata")
public class PdfMetadata {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "media_id", nullable = false, unique = true)
    private Media media;

    private String title;

    private String author;

    private int pageCount;
}
