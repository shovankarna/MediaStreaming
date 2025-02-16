package com.media.MediaStreaming.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoMetadata {
    private double duration;
    private String videoCodec;
    private int width;
    private int height;
    private String audioCodec;
    private int audioBitrate;
    private int videoBitrate;
}