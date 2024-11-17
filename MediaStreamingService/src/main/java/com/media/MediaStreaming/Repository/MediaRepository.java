package com.media.MediaStreaming.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.media.MediaStreaming.Models.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {
    
}
