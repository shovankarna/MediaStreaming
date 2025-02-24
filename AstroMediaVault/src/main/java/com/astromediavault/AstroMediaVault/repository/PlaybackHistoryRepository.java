package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.PlaybackHistory;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlaybackHistoryRepository extends JpaRepository<PlaybackHistory, UUID> {
    List<PlaybackHistory> findByUserId(UUID userId);
    List<PlaybackHistory> findByMediaId(UUID mediaId);
}
