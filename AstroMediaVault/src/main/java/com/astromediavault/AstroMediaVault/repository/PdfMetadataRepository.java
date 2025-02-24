package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.PdfMetadata;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PdfMetadataRepository extends JpaRepository<PdfMetadata, UUID> {
    Optional<PdfMetadata> findByMediaId(UUID mediaId);
}
