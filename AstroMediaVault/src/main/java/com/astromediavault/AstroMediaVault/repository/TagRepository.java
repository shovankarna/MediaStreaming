package com.astromediavault.AstroMediaVault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.astromediavault.AstroMediaVault.model.Tag;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);
}
