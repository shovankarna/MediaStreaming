package com.astromediavault.AstroMediaVault.controller;

import com.astromediavault.AstroMediaVault.dto.SubtitleResponse;
import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import com.astromediavault.AstroMediaVault.service.SubtitleService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
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

    private static final Logger logger = LoggerFactory.getLogger(VideoPlayerController.class);

    @GetMapping("/{mediaId}")
    public String getVideoPlayer(@PathVariable UUID mediaId, Model model) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));

        // ✅ OS-independent HLS URL
        String streamUrl = Paths.get("users", media.getUser().getId().toString(), "videos", "hls",
                media.getId().toString(), "master.m3u8")
                .toString()
                .replace("\\", "/"); // Ensure proper URL format

        streamUrl = serverHost + "/" + streamUrl;

        // ✅ Fetch subtitles (ensure non-null)
        List<SubtitleResponse> subtitles = subtitleService.getSubtitlesForMedia(media.getId());
        if (subtitles == null) {
            subtitles = new ArrayList<>();
        }

        // 🔴 Debugging Logs
        logger.info("Stream URL: {}", streamUrl);
        subtitles.forEach(subtitle -> logger.info("Subtitle: ID={} Language={} URL={}",
                subtitle.getId(), subtitle.getLanguage(), subtitle.getSubtitleUrl()));

        // ✅ Add data to model
        model.addAttribute("streamUrl", streamUrl);
        model.addAttribute("subtitles", subtitles);

        return "video-player"; // 🎥 Thymeleaf template
    }
}
