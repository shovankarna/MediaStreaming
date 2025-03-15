package com.astromediavault.AstroMediaVault.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.astromediavault.AstroMediaVault.dto.SubtitleResponse;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.model.Subtitle;
import com.astromediavault.AstroMediaVault.repository.SubtitleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubtitleService {

    private final SubtitleRepository subtitleRepository;

    private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

    @Value("${storage.local.path}")
    private String localStoragePath;

    @Value("${server.host}")
    private String serverHost;

    public void saveSubtitle(MultipartFile subtitleFile, String language, Media media) throws IOException {
        String subtitleFileName = UUID.randomUUID() + "_" + subtitleFile.getOriginalFilename();
        String subtitleFolder = Paths.get("users", media.getUser().getId().toString(), "subtitles").toString();

        File directory = new File(Paths.get(localStoragePath, subtitleFolder).toString());
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create subtitle directory: " + subtitleFolder);
        }

        // Save subtitle file
        String subtitlePath = Paths.get(subtitleFolder, subtitleFileName).toString();
        File localSubtitleFile = new File(Paths.get(localStoragePath, subtitlePath).toString());
        subtitleFile.transferTo(localSubtitleFile);

        // Save subtitle metadata
        Subtitle subtitle = new Subtitle();
        subtitle.setMedia(media);
        subtitle.setLanguage(language);
        subtitle.setSubtitlePath(subtitlePath);
        subtitleRepository.save(subtitle);

        logger.info("Subtitle uploaded successfully: {}", subtitlePath);
    }

    public List<SubtitleResponse> getSubtitlesForMedia(UUID mediaId) {
        List<Subtitle> subtitles = subtitleRepository.findByMediaId(mediaId);

        return subtitles.stream()
                .map(subtitle -> new SubtitleResponse(
                        subtitle.getId(),
                        subtitle.getLanguage(),
                        buildSubtitleUrl(subtitle.getSubtitlePath()) 
                ))
                .collect(Collectors.toList());
    }

    private String buildSubtitleUrl(String subtitlePath) {
        return serverHost + "/" + subtitlePath.replace("\\", "/");
    }
}
