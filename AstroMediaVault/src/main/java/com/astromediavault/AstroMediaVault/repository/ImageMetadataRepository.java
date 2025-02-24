package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.ImageMetadata;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, UUID> {
    Optional<ImageMetadata> findByMediaId(UUID mediaId);
}