// package com.astromediavault.AstroMediaVault.config;

// import io.minio.MinioClient;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.beans.factory.annotation.Value;

// @Configuration
// public class MinioConfig {

//     @Value("${spring.minio.url}")
//     private String minioUrl;

//     @Value("${spring.minio.access-key}")
//     private String minioAccessKey;

//     @Value("${spring.minio.secret-key}")
//     private String minioSecretKey;

//     @Value("${storage.local.path}") // Use local storage directory
//     private String localStoragePath;

//     @Bean
//     public MinioClient minioClient() {
//         return MinioClient.builder()
//                 .endpoint("http://localhost:9000") // Keep MinIO running locally
//                 .credentials(minioAccessKey, minioSecretKey)
//                 .build();
//     }

//     public String getLocalStoragePath() {
//         return localStoragePath;
//     }
// }