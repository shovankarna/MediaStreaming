package com.media.MediaStreaming.Models;

import lombok.Data;

public enum MediaStatus {
    UPLOADED("Uploaded"),
    PROCESSING("Processing"),
    PROCESSED("Processed"),
    FAILED("Failed"),
    DELETED("Deleted");

    private final String status;

    MediaStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return this.status;
    }
}
