package com.media.MediaStreaming.Controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.media.MediaStreaming.Models.MediaType;
import com.media.MediaStreaming.Services.FileStorageService;
import com.media.MediaStreaming.Services.VideoProcessingService;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/media")
    public ResponseEntity<String> uploadMedia(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }

        try {
            // Delegate the upload and processing logic to the service
            String result = fileStorageService.storeFile(file, "defaultUserId");

            return ResponseEntity.ok("File uploaded successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("File upload failed: " + e.getMessage());
        }
    }
}