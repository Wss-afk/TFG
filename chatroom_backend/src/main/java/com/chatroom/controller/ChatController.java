package com.chatroom.controller;

import com.chatroom.entity.Message;
import com.chatroom.entity.User;
import com.chatroom.entity.Group;
import com.chatroom.repository.MessageRepository;
import com.chatroom.repository.UserRepository;
import com.chatroom.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;

    @MessageMapping("/chat/single")
    public void sendSingleMessage(@Payload Message message) {
        message.setTimestamp(LocalDateTime.now());
        messageRepository.save(message);
        if (message.getReceiver() != null) {
            messagingTemplate.convertAndSendToUser(
                message.getReceiver().getUsername(),
                "/queue/messages",
                message
            );
        }
    }

    @MessageMapping("/chat/group")
    public void sendGroupMessage(@Payload Message message) {
        message.setTimestamp(LocalDateTime.now());
        messageRepository.save(message);
        if (message.getGroup() != null) {
            Optional<Group> groupOpt = groupRepository.findById(message.getGroup().getId());
            groupOpt.ifPresent(group -> {
                group.getUsers().forEach(user -> {
                    messagingTemplate.convertAndSendToUser(
                        user.getUsername(),
                        "/queue/messages",
                        message
                    );
                });
            });
        }
    }

    @GetMapping("/messages")
    public List<Message> getMessages(@RequestParam(required = false) Long groupId, @RequestParam(required = false) Long receiverId) {
        return messageRepository.findByGroupIdOrReceiverId(groupId, receiverId);
    }

    @PostMapping("/send")
    public Message sendMessage(@RequestBody Message message) {
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }
}