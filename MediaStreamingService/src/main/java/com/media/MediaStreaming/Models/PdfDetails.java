package com.media.MediaStreaming.Models;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "pdf_details")
public class PdfDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pdfId;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "media_id", referencedColumnName = "mediaId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Media media;

    private Integer pageCount;

    private Integer sizeInKb;
}
