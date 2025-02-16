package com.media.MediaStreaming.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.media.MediaStreaming.Models.VideoSegment;

public interface VideoSegmentRepository extends JpaRepository<VideoSegment, Long> {

}
