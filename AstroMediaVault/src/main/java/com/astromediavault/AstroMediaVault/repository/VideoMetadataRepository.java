package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.VideoMetadata;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, UUID> {
    Optional<VideoMetadata> findByMediaId(UUID mediaId);

    void deleteByMediaId(UUID mediaId);
}
