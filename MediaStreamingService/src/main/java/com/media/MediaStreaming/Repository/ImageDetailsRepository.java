package com.media.MediaStreaming.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.media.MediaStreaming.Models.ImageDetails;

public interface ImageDetailsRepository extends JpaRepository<ImageDetails, Long> {
    
}

