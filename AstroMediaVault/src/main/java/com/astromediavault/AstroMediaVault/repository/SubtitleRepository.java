package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.Subtitle;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubtitleRepository extends JpaRepository<Subtitle, UUID> {
    List<Subtitle> findByMediaId(UUID mediaId);
}