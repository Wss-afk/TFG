package com.chatroom.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "group_read_status", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "group_id"})
})
public class GroupReadStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
}
