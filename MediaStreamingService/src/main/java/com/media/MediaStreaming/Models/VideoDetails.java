package com.media.MediaStreaming.Models;

import java.util.List;
import java.util.ArrayList;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "video_details")
public class VideoDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long videoId;

    @Column(nullable = false)
    private String uniqueVideoId; // Add this field

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "media_id", referencedColumnName = "mediaId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Media media;

    private String originalVideoPath;
    private String segmentedFolderPath;
    private String resolution;
    private double duration;
    private String codec;
    private String hlsPlaylistPath;

    @OneToMany(mappedBy = "videoDetails", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoSegment> videoSegments = new ArrayList<>();
}
