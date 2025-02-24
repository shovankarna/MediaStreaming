package com.astromediavault.AstroMediaVault.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MediaUploadRequest {
    private String title;
    private String description;
    private String fileType;
    private MultipartFile file;
}
