package com.chatroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Data
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    // Actor (admin)
    private Long actorId;
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    private Role actorRole;

    // Action metadata
    @Column(nullable = false, length = 100)
    private String action; // e.g., USER_CREATE, PASSWORD_CHANGE

    @Column(length = 50)
    private String targetType; // USER, GROUP, SYSTEM
    private Long targetId;
    private String targetName; // username or group name

    @Column(nullable = false)
    private boolean success = true;

    private Integer statusCode; // HTTP status

    @Column(length = 255)
    private String ip;

    @Column(length = 255)
    private String userAgent;

    @Lob
    private String details; // JSON string with additional info
}