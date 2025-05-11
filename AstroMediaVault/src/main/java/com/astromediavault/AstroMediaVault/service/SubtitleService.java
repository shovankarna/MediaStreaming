package com.astromediavault.AstroMediaVault.service;

import com.astromediavault.AstroMediaVault.dto.SubtitleResponse;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.model.Subtitle;
import com.astromediavault.AstroMediaVault.repository.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubtitleService {

    private final SubtitleRepository subtitleRepository;
    private static final Logger logger = LoggerFactory.getLogger(SubtitleService.class);

    @Value("${storage.local.path}")
    private String localStoragePath;

    @Value("${server.host}")
    private String serverHost;

    /**
     * Save Subtitle File
     */
    public void saveSubtitle(MultipartFile subtitleFile, String language, Media media) throws IOException {
        // Use UUID_filename format
        String subtitleFileName = media.getId() + "_" + subtitleFile.getOriginalFilename();

        // New subtitle folder inside the video folder using mediaId
        String subtitleFolder = Paths.get(
                "users",
                media.getUser().getId().toString(),
                "videos",
                "subtitles").toString();

        File directory = new File(Paths.get(localStoragePath, subtitleFolder).toString());
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Failed to create subtitle directory: " + subtitleFolder);
        }

        // Save subtitle file to the newly structured path
        String subtitlePath = Paths.get(subtitleFolder, subtitleFileName).toString();
        File localSubtitleFile = new File(Paths.get(localStoragePath, subtitlePath).toString());
        subtitleFile.transferTo(localSubtitleFile);

        // Save subtitle metadata to DB
        Subtitle subtitle = new Subtitle();
        subtitle.setMedia(media);
        subtitle.setLanguage(language);
        subtitle.setSubtitlePath(subtitlePath);
        subtitleRepository.save(subtitle);

        logger.info("Subtitle uploaded and saved under video folder: {}", subtitlePath);
    }

    /**
     * Get Subtitles for a Video
     */
    public List<SubtitleResponse> getSubtitlesForMedia(UUID mediaId) {
        List<Subtitle> subtitles = subtitleRepository.findByMediaId(mediaId);

        return subtitles.stream()
                .map(subtitle -> new SubtitleResponse(
                        subtitle.getId(),
                        subtitle.getLanguage(),
                        buildSubtitleUrl(subtitle.getSubtitlePath())))
                .collect(Collectors.toList());
    }

    /**
     * Build Subtitle URL
     */
    private String buildSubtitleUrl(String subtitlePath) {
        return serverHost + "/" + subtitlePath.replace("\\", "/");
    }

    /**
     * Delete Subtitles for a Video
     */
    public void deleteSubtitlesForMedia(UUID mediaId) {
        List<Subtitle> subtitles = subtitleRepository.findByMediaId(mediaId);
        for (Subtitle subtitle : subtitles) {
            Path subtitlePath = Paths.get(localStoragePath, subtitle.getSubtitlePath()).normalize(); // ✅ FIXED
            logger.info("Deleting subtitle file: {}", subtitlePath);
            deleteLocalFile(subtitlePath.toString());
        }
        subtitleRepository.deleteByMediaId(mediaId);
        logger.info("Deleted all subtitles for media ID: {}", mediaId);
    }

    /**
     * Delete Local File Helper Method
     */
    /**
     * Delete Local File Helper Method (Improved)
     */
    private void deleteLocalFile(String filePath) {
        File file = new File(filePath);

        // Debugging logs
        if (!file.exists()) {
            logger.warn("File does not exist, skipping deletion: {}", filePath);
            return;
        }

        if (!file.canWrite()) {
            logger.error("No write permission for file: {}", filePath);
            return;
        }

        if (file.delete()) {
            logger.info("Deleted file successfully: {}", filePath);
        } else {
            logger.error("Failed to delete file: {}", filePath);
        }
    }
}
