package com.media.MediaStreaming.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.media.MediaStreaming.Models.PdfDetails;

public interface PdfDetailsRepository extends JpaRepository<PdfDetails, Long> {
    
}
