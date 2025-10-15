package com.chatroom.controller;

import com.chatroom.entity.Message;
import com.chatroom.entity.Group;
import com.chatroom.repository.MessageRepository;
import com.chatroom.repository.UserRepository;
import com.chatroom.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
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
                    message);
        }
        if (message.getSender() != null) {
            messagingTemplate.convertAndSendToUser(
                    message.getSender().getUsername(),
                    "/queue/messages",
                    message);
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
                            message);
                });
            });
        }
    }

    @GetMapping("/messages")
    public List<Message> getMessages(@RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long receiverId, @RequestParam(required = false) Long userId) {
        try {
            if (groupId != null) {
                Group group = groupRepository.findById(groupId).orElse(null);
                if (group != null) {
                    return messageRepository.findByGroup(group);
                } else {
                    return List.of();
                }
            } else if (receiverId != null && userId != null) {
                if (userRepository.findById(userId).isEmpty() || userRepository.findById(receiverId).isEmpty()) {
                    return List.of();
                }
                return messageRepository.findChatHistory(userId, receiverId);
            } else {
                return List.of();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @PostMapping("/send")
    public Message sendMessage(@RequestBody Message message, @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long receiverId) {
        // Asignar sender y receiver correctamente usando los IDs si están presentes
        if (message.getSender() == null && senderId != null) {
            message.setSender(userRepository.findById(senderId).orElse(null));
        }
        if (message.getReceiver() == null && receiverId != null) {
            message.setReceiver(userRepository.findById(receiverId).orElse(null));
        }
        if (message.getGroup() != null && message.getGroup().getId() != null) {
            message.setGroup(groupRepository.findById(message.getGroup().getId()).orElse(null));
        }
        message.setTimestamp(LocalDateTime.now());
        Message savedMessage = messageRepository.save(message);
        
        // 通过WebSocket推送消息给接收者
        if (savedMessage.getReceiver() != null) {
            messagingTemplate.convertAndSendToUser(
                    savedMessage.getReceiver().getUsername(),
                    "/queue/messages",
                    savedMessage);
        }
        
        return savedMessage;
    }

    @PostMapping("/mark-read")
    public ResponseEntity<String> markMessagesAsRead(@RequestParam Long userId, @RequestParam Long senderId) {
        try {
            List<Message> unreadMessages = messageRepository.findUnreadMessages(userId, senderId);
            for (Message message : unreadMessages) {
                message.setIsRead(true);
                messageRepository.save(message);
            }
            return ResponseEntity.ok("Messages marked as read");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error marking messages as read");
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@RequestParam Long userId, @RequestParam Long senderId) {
        try {
            int count = messageRepository.countUnreadMessages(userId, senderId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(-1);
        }
    }
}