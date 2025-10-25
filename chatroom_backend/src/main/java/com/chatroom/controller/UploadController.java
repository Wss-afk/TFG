package com.chatroom.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class UploadController {

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Archivo vacío");
        }
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            boolean isImage = contentType.startsWith("image/");
            String kind = isImage ? "images" : "files";

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                ext = originalName.substring(dot);
            }
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String filename = uuid + ext;

            Path targetDir = Paths.get("uploads", "messages", kind);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            String url = "/uploads/messages/" + kind + "/" + filename;
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