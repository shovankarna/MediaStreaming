package com.astromediavault.AstroMediaVault.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamingResponse {
    private String streamUrl;
    private List<SubtitleResponse> subtitles;
}

