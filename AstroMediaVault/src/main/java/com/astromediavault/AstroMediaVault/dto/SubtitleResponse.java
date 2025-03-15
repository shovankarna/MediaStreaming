package com.astromediavault.AstroMediaVault.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleResponse {
    private UUID id;
    private String language;
    private String subtitleUrl;

    @Override
    public String toString() {
        return "{ \"id\": \"" + id + "\", \"language\": \"" + language + "\", \"subtitleUrl\": \"" + subtitleUrl
                + "\" }";
    }
}
