package com.astromediavault.AstroMediaVault.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {
    private UUID id;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String storagePath;
    private Instant uploadTimestamp;
}
