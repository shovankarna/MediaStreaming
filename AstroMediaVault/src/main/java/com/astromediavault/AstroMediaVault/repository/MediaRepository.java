package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.Media;
import com.astromediavault.AstroMediaVault.model.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {
    List<Media> findByUserId(UUID userId);
    List<Media> findByFileType(Media.FileType fileType);
    List<Media> findByUser(User user);

}