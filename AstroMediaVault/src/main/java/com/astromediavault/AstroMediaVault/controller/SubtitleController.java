package com.astromediavault.AstroMediaVault.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.astromediavault.AstroMediaVault.dto.ApiResponse;
import com.astromediavault.AstroMediaVault.dto.SubtitleResponse;
import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import com.astromediavault.AstroMediaVault.service.MediaService;
import com.astromediavault.AstroMediaVault.service.SubtitleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subtitles")
@RequiredArgsConstructor
public class SubtitleController {

    private final SubtitleService subtitleService;
    private final MediaRepository mediaRepository;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<String>> addSubtitle(
            @RequestParam("mediaId") UUID mediaId,
            @RequestParam("subtitle") MultipartFile subtitleFile,
            @RequestParam("language") String language) {

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        try {
            subtitleService.saveSubtitle(subtitleFile, language, media);
            return ResponseEntity.ok(ApiResponse.success("Subtitle added successfully!", null));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload subtitle."));
        }
    }

    @GetMapping("/fetch/{mediaId}")
    public ResponseEntity<ApiResponse<List<SubtitleResponse>>> getSubtitlesByMediaId(@PathVariable UUID mediaId) {
        List<SubtitleResponse> subtitles = subtitleService.getSubtitlesForMedia(mediaId);
        return ResponseEntity.ok(ApiResponse.success("Subtitles retrieved successfully", subtitles));
    }
    

}
