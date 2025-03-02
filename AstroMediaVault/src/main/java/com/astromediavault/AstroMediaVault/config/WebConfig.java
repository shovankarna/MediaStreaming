package com.astromediavault.AstroMediaVault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${storage.local.path}")
    private String localStoragePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure path ends with a trailing slash
        String fullPath = localStoragePath.endsWith("/") ? localStoragePath : localStoragePath + "/";
        String fullHlsPath = "file:///" + fullPath.replace("\\", "/");

        // Map /users/** to the root of your storage directory
        registry.addResourceHandler("/users/**")
                .addResourceLocations(fullHlsPath + "users/");

        System.out.println("Serving HLS files from: " + fullHlsPath + "users/");
    }

    @Bean
    public MimeMappings mimeMappings() {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
        mappings.add("m3u8", "application/vnd.apple.mpegurl");
        mappings.add("ts", "video/mp2t");
        return mappings;
    }
}
