package com.chatroom.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${app.upload-dir:}")
    private String uploadDirProp;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mapear /uploads/** a una carpeta estable en el sistema
        Path base = resolveUploadBase();
        String uploadPath = base.toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }

    private Path resolveUploadBase() {
        try {
            if (uploadDirProp != null && !uploadDirProp.isBlank()) {
                Path p = Paths.get(uploadDirProp);
                Files.createDirectories(p);
                return p;
            }
        } catch (Exception ignored) {}

        Path p = Paths.get(System.getProperty("user.home"), "chatroom_uploads");
        try { Files.createDirectories(p); } catch (IOException ignored) {}
        return p;
    }
}
