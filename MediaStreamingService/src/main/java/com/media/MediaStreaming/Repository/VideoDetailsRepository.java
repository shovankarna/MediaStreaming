package com.media.MediaStreaming.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.media.MediaStreaming.Models.VideoDetails;

public interface VideoDetailsRepository extends JpaRepository<VideoDetails, Long> {
    
}

