package com.astromediavault.AstroMediaVault.consumer;

import org.springframework.stereotype.Service;

import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class PDFPreviewGeneartionConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PDFPreviewGeneartionConsumer.class);

    private final MediaRepository mediaRepository;

    @Value("${storage.local.path}")
    private String localStoragePath;

    @RabbitListener(queues = "pdf-preview-generation-queue")
    public void generatePreview(String mediaIdStr) {
        try {
            UUID mediaId = UUID.fromString(mediaIdStr);
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new MediaNotFoundException("Media not found: " + mediaId));

            String pdfPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
            try (PDDocument document = PDDocument.load(new File(pdfPath))) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                BufferedImage image = pdfRenderer.renderImageWithDPI(0, 150); // first page, 150 DPI

                String previewPath = Paths.get("users", media.getUser().getId().toString(), "pdfs", "previews").toString();
                Path fullPreviewDir = Paths.get(localStoragePath, previewPath);
                Files.createDirectories(fullPreviewDir);

                String previewFileName = media.getId().toString() + ".png";
                Path previewFilePath = fullPreviewDir.resolve(previewFileName);

                ImageIO.write(image, "png", previewFilePath.toFile());

                logger.info("Generated PDF preview for mediaId={} at {}", mediaId, previewFilePath);
            }
        } catch (Exception e) {
            logger.error("Failed to generate PDF preview: {}", e.getMessage(), e);
        }
    }
}
