package com.media.MediaStreaming.Models;

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
@Table(name = "video_segments")
public class VideoSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long segmentId; // Unique identifier for each video segment

    @ManyToOne(optional = false)
    @JoinColumn(name = "video_id", referencedColumnName = "videoId", nullable = false)
    private VideoDetails videoDetails; // Link to the parent video

    private String resolution; // e.g., "1080p", "720p", "480p"
    private String path; // Path to the segmented file
    private double duration; // Duration of the segment in seconds
    private String codec; // Codec used (e.g., "h264")

    // Other metadata as required
}