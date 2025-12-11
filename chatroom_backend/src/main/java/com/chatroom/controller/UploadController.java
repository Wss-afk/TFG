package com.chatroom.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class UploadController {

    @Value("${app.upload-dir:}")
    private String uploadDirProp;

    private Path baseDir() {
        try {
            if (uploadDirProp != null && !uploadDirProp.isBlank()) {
                Path p = Paths.get(uploadDirProp);
                if (!p.isAbsolute()) {
                    p = Paths.get("").resolve(uploadDirProp).toAbsolutePath().normalize();
                }
                Files.createDirectories(p);
                return p;
            }
        } catch (Exception ignored) {}
        Path p = Paths.get(System.getProperty("user.home"), "chatroom_uploads");
        try { Files.createDirectories(p); } catch (IOException ignored) {}
        return p;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "kind", required = false) String kindOverride) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Archivo vacío");
        }
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            boolean isImage = contentType.startsWith("image/");
            String kind = (kindOverride != null && !kindOverride.isBlank())
                    ? kindOverride
                    : (isImage ? "images" : "files");

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                ext = originalName.substring(dot);
            }
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String filename = uuid + ext;

            Path targetDir = "avatar".equalsIgnoreCase(kind)
                    ? baseDir().resolve("avatars")
                    : baseDir().resolve("messages").resolve(kind);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = "avatar".equalsIgnoreCase(kind)
                    ? "/uploads/avatars/" + filename
                    : "/uploads/messages/" + kind + "/" + filename;
            Map<String, Object> body = new HashMap<>();
            body.put("url", url);
            body.put("type", isImage ? "image" : "file");
            body.put("name", originalName);
            body.put("size", file.getSize());

            return ResponseEntity.ok(body);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error al subir archivo: " + e.getMessage());
        }
    }
}
