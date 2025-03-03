package com.astromediavault.AstroMediaVault.controller;

import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/player")
@RequiredArgsConstructor
public class VideoPlayerController {

    private final MediaRepository mediaRepository;

    @Value("${server.host}") // Backend URL from application.yml
    private String serverHost;

    @GetMapping("/{mediaId}")
    public String getVideoPlayer(@PathVariable UUID mediaId, Model model) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));

        String streamUrl = serverHost + "/users/" + media.getUser().getId() +
                "/videos/hls/" + media.getId() + "/master.m3u8";

        model.addAttribute("streamUrl", streamUrl);
        return "video-player"; // This loads templates/video-player.html
    }
}
