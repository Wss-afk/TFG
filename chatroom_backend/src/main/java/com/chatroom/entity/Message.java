package com.chatroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver; // 单聊时使用

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group; // 群聊时使用

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String type; // text, image, file

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private String fileUrl;

    @Column
    private Boolean isRead = false;
}