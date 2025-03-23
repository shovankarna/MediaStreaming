package com.astromediavault.AstroMediaVault.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

import com.astromediavault.AstroMediaVault.exception.InvalidFileTypeException;
import com.astromediavault.AstroMediaVault.exception.MediaNotFoundException;
import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.model.PdfMetadata;
import com.astromediavault.AstroMediaVault.repository.MediaRepository;
import com.astromediavault.AstroMediaVault.repository.PdfMetadataRepository;

@Service
@RequiredArgsConstructor
public class PDFService {

    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);
    private final MediaRepository mediaRepository;
    private final PdfMetadataRepository pdfMetadataRepository;

    @Value("${storage.local.path}")
    private String localStoragePath;

    @Value("${media.pdf.max-size-bytes:10485760}") // Default 10MB
    private long maxPdfFileSize;

    /**
     * Handles PDF Upload, Validation, Metadata Extraction, and Saving PDF Metadata
     */
    public void processPdfUpload(Media media, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidFileTypeException("Uploaded file is empty");
        }
        if (file.getSize() > maxPdfFileSize) {
            throw new InvalidFileTypeException("PDF file exceeds maximum allowed size of " + maxPdfFileSize + " bytes");
        }
        logger.debug("Attempting to load PDF: originalFilename={}, size={}", file.getOriginalFilename(),
                file.getSize());

        // Instead of transferring to a temporary file, read the input stream directly
        try (InputStream inputStream = file.getInputStream();
                PDDocument document = PDDocument.load(inputStream)) {

            if (document.isEncrypted()) {
                throw new InvalidFileTypeException("Encrypted PDF files are not supported");
            }
            // Extract metadata
            PDDocumentInformation info = document.getDocumentInformation();
            int pageCount = document.getNumberOfPages();
            String title = info.getTitle();
            String author = info.getAuthor();
            PdfMetadata pdfMetadata = new PdfMetadata();
            pdfMetadata.setMedia(media);
            pdfMetadata.setTitle(title);
            pdfMetadata.setAuthor(author);
            pdfMetadata.setPageCount(pageCount);
            pdfMetadataRepository.save(pdfMetadata);
            logger.info("PDF metadata extracted and saved for mediaId={}", media.getId());
        } catch (IOException e) {
            logger.error("Failed to process PDF file: {}", e.getMessage(), e);
            throw new InvalidFileTypeException("Uploaded file is not a valid PDF");
        }
    }

    /**
     * Return the preview image for the first page of the PDF
     */
    public ResponseEntity<Resource> getPdfPreview(UUID mediaId) throws IOException {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        if (media.getFileType() != Media.FileType.PDF) {
            throw new InvalidFileTypeException("Preview is only available for PDFs.");
        }

        String previewPath = Paths.get(localStoragePath, "users",
                media.getUser().getId().toString(), "pdfs", "previews", mediaId.toString() + ".png").toString();

        File file = new File(previewPath);
        if (!file.exists()) {
            throw new MediaNotFoundException("PDF preview not found for media ID: " + mediaId);
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.png\"")
                .body(resource);
    }

    /**
     * Download the original PDF file
     */
    public ResponseEntity<Resource> downloadPdf(UUID mediaId) throws IOException {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        if (media.getFileType() != Media.FileType.PDF) {
            throw new InvalidFileTypeException("Download is only available for PDFs.");
        }

        String fullPath = Paths.get(localStoragePath, media.getStoragePath()).toString();
        File file = new File(fullPath);
        if (!file.exists()) {
            throw new MediaNotFoundException("PDF file not found on disk: " + fullPath);
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + media.getFileName() + "\"")
                .body(resource);
    }

    /**
     * Delete all PDF-related files and metadata
     */
    public void deletePdfFiles(UUID mediaId) {
        logger.info("🔹 Deleting PDF files for media ID: {}", mediaId);

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new MediaNotFoundException("Media not found with ID: " + mediaId));

        // 🔹 Delete preview image
        Path previewPath = Paths.get(localStoragePath, "users",
                media.getUser().getId().toString(), "pdfs", "previews", mediaId + ".png").normalize();
        deleteLocalFile(previewPath);

        // 🔹 Delete original PDF
        Path originalPath = Paths.get(localStoragePath, media.getStoragePath()).normalize();
        deleteLocalFile(originalPath);

        // 🔹 Delete PDF metadata
        pdfMetadataRepository.deleteByMediaId(mediaId);

        logger.info("✅ Successfully deleted all PDF-related files for media ID: {}", mediaId);
    }

    /**
     * Delete Local File Helper Method
     */
    private void deleteLocalFile(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("✅ Deleted file: {}", filePath);
            } else {
                logger.warn("⚠️ File not found, skipping deletion: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("❌ Failed to delete file: {} - {}", filePath, e.getMessage(), e);
        }
    }
}
