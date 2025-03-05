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

        // Serve HLS video segments
        registry.addResourceHandler("/users/**")
                .addResourceLocations(fullHlsPath + "users/")
                .setCachePeriod(3600); // Cache for 1 hour

        // Serve static files like CSS and JS
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);

        System.out.println("Serving HLS files from: " + fullHlsPath + "users/");
    }

    @Bean
    public MimeMappings mimeMappings() {
        MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
        mappings.add("m3u8", "application/vnd.apple.mpegurl");
        mappings.add("ts", "video/mp2t");
        mappings.add("css", "text/css");
        mappings.add("js", "application/javascript");
        return mappings;
    }
}
