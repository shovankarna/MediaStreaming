package com.astromediavault.AstroMediaVault.controller;

import com.astromediavault.AstroMediaVault.dto.SubtitleResponse;
import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import com.astromediavault.AstroMediaVault.service.SubtitleService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/player")
@RequiredArgsConstructor
public class VideoPlayerController {

    private final MediaRepository mediaRepository;
    private final SubtitleService subtitleService;

    @Value("${server.host}") // Backend URL from application.yml
    private String serverHost;

    @GetMapping("/{mediaId}")
    public String getVideoPlayer(@PathVariable UUID mediaId, Model model) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));

        String streamUrl = serverHost + "/users/" + media.getUser().getId() +
                "/videos/hls/" + media.getId() + "/master.m3u8";

        // Fetch subtitles
        List<SubtitleResponse> subtitles = subtitleService.getSubtitlesForMedia(media.getId());
        if (subtitles == null) {
            subtitles = new ArrayList<>(); // Ensure it's always a list, never null
        }

        // 🔴 Debugging Logs
        System.out.println("Stream URL: " + streamUrl);
        subtitles.forEach(subtitle -> {
            System.out.println("Subtitle ID: " + subtitle.getId());
            System.out.println("Subtitle Language: " + subtitle.getLanguage());
            System.out.println("Subtitle URL: " + subtitle.getSubtitleUrl());
        });

        model.addAttribute("streamUrl", streamUrl);
        model.addAttribute("subtitles", subtitles); // Ensure it's always passed

        return "video-player";
    }

}
