package com.astromediavault.AstroMediaVault.controller;

import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/pdf-viewer")
@RequiredArgsConstructor
public class PdfViewerController {

    private final MediaRepository mediaRepository;

    @Value("${server.host}")
    private String serverHost;

    private static final Logger logger = LoggerFactory.getLogger(PdfViewerController.class);

    @GetMapping("/{mediaId}")
    public String viewPdf(@PathVariable UUID mediaId, Model model) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("PDF not found: " + mediaId));

        if (media.getFileType() != Media.FileType.PDF) {
            throw new MediaNotFoundException("Media is not a PDF: " + mediaId);
        }

        String pdfUrl = serverHost + "/" + media.getStoragePath().replace("\\", "/");

        logger.info("Rendering PDF Viewer for URL: {}", pdfUrl);
        model.addAttribute("pdfUrl", pdfUrl);
        return "pdf-viewer"; // ⬅️ Thymeleaf view name
    }
}
