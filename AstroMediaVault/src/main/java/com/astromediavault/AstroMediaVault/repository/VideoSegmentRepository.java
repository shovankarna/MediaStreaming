package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.VideoSegment;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoSegmentRepository extends JpaRepository<VideoSegment, UUID> {
    List<VideoSegment> findByMediaId(UUID mediaId);

    void deleteByMediaId(UUID mediaId);
}
