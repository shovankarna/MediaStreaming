package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.MediaTag;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaTagRepository extends JpaRepository<MediaTag, UUID> {
    List<MediaTag> findByMediaId(UUID mediaId);

    void deleteByMediaId(UUID mediaId);
}
