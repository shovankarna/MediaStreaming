package com.astromediavault.AstroMediaVault.controller;

import java.io.File;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Value("${storage.local.path}")
    private String localStoragePath;

    @GetMapping("/check-hls/{userId}/{mediaId}")
    public ResponseEntity<String> checkHlsFile(
            @PathVariable String userId,
            @PathVariable String mediaId) {

        String hlsPath = Paths.get(localStoragePath, "users", userId, "videos", "hls", mediaId, "master.m3u8").toString();
        File file = new File(hlsPath);

        if (file.exists()) {
            return ResponseEntity.ok("HLS file exists: " + file.getAbsolutePath());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("HLS file not found: " + file.getAbsolutePath());
        }
    }
}
