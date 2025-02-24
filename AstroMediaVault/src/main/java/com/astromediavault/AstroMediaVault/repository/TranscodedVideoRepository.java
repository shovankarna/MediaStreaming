package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.TranscodedVideo;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscodedVideoRepository extends JpaRepository<TranscodedVideo, UUID> {
    List<TranscodedVideo> findByMediaId(UUID mediaId);
}
