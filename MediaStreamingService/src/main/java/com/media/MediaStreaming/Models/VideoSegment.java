package com.media.MediaStreaming.Models;

import lombok.Data;

@Data
public class VideoSegment {

    private String resolution;
    private String path;
    private double duration;
    private String codec;
}
