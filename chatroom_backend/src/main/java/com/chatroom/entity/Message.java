package com.chatroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Data
@Table(name = "messages")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id")
    @JsonIgnoreProperties({"password"})
    private User sender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id")
    @JsonIgnoreProperties({"password"})
    private User receiver; // 单聊时使用

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    @JsonIgnoreProperties({"users"})
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