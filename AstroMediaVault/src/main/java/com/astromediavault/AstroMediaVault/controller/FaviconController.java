package com.astromediavault.AstroMediaVault.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("favicon.ico")
public class FaviconController {

    @GetMapping
    public ResponseEntity<Void> returnNoFavicon() {
        return ResponseEntity.notFound().build(); // Return 404 but without logging an error
    }
}
