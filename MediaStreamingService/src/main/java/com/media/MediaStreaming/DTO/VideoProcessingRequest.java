package com.media.MediaStreaming.DTO;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class VideoProcessingRequest {
    private String filePath;
    private String outputPath;
    private String uniqueVideoId; // Added this field
    private String userId;
    private boolean keepOriginal;
    private String mediaId;
    private List<String> targetResolutions; // Added to specify required resolutions
}