package com.chatroom.controller;

import com.chatroom.entity.Message;
import com.chatroom.entity.Group;
import com.chatroom.entity.User;
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

import java.util.Map;
import java.util.HashMap;

import java.util.ArrayList;

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
    @Autowired
    private com.chatroom.repository.GroupReadStatusRepository groupReadStatusRepository;

    @GetMapping("/notifications")
    public ResponseEntity<List<Message>> getNotifications(@RequestParam Long userId) {
        try {
            List<Message> notifications = new ArrayList<>();
            
            // 1. Direct Messages
            List<Message> directMessages = messageRepository.findByReceiverIdAndIsReadFalse(userId);
            notifications.addAll(directMessages);
            
            // 2. Group Messages
            List<Group> allGroups = groupRepository.findAll();
            for (Group g : allGroups) {
                if (g.getUsers() != null && g.getUsers().stream().anyMatch(u -> u.getId().equals(userId))) {
                    Optional<com.chatroom.entity.GroupReadStatus> statusOpt = groupReadStatusRepository.findByUserIdAndGroupId(userId, g.getId());
                    Long lastReadId = statusOpt.map(com.chatroom.entity.GroupReadStatus::getLastReadMessageId).orElse(0L);
                    
                    List<Message> groupMessages = messageRepository.findGroupUnreadMessages(g.getId(), lastReadId, userId);
                    notifications.addAll(groupMessages);
                }
            }
            
            // Sort by timestamp desc
            notifications.sort((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()));
            
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @MessageMapping("/chat/single")
    public void sendSingleMessage(@Payload Message message) {
        // Bloquear envío si el remitente está deshabilitado
        if (isSenderDisabled(message)) {
            return;
        }
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
        // Bloquear envío si el remitente está deshabilitado
        if (isSenderDisabled(message)) {
            return;
        }
        // Bloquear envío si el remitente no pertenece al grupo
        try {
            if (message.getGroup() != null && message.getGroup().getId() != null && message.getSender() != null) {
                Long groupId = message.getGroup().getId();
                Optional<Group> groupOpt = groupRepository.findById(groupId);
                if (groupOpt.isPresent()) {
                    Group g = groupOpt.get();
                    final Long senderIdFinal = (message.getSender() != null && message.getSender().getId() != null)
                            ? message.getSender().getId()
                            : (message.getSender() != null && message.getSender().getUsername() != null)
                                ? userRepository.findByUsername(message.getSender().getUsername()).map(User::getId).orElse(null)
                                : null;
                    boolean isMember = senderIdFinal != null && g.getUsers() != null && g.getUsers().stream().anyMatch(u -> u.getId() != null && u.getId().equals(senderIdFinal));
                    if (!isMember) {
                        return; // no guardar ni difundir
                    }
                } else {
                    return; // grupo inexistente
                }
            }
        } catch (Exception ignored) {}
        message.setTimestamp(LocalDateTime.now());
        messageRepository.save(message);
        if (message.getGroup() != null) {
            // Solo enviar al canal del grupo, no a usuarios individuales
            messagingTemplate.convertAndSend("/topic/group/" + message.getGroup().getId(), message);
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<List<Message>> getMessages(@RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long receiverId, @RequestParam(required = false) Long userId) {
        try {
            if (groupId != null) {
                Optional<Group> groupOpt = groupRepository.findById(groupId);
                if (groupOpt.isPresent()) {
                    // Validar que el userId pertenece al grupo; si no viene, negar
                    if (userId == null) {
                        return ResponseEntity.ok(List.of());
                    }
                    Group g = groupOpt.get();
                    boolean isMember = g.getUsers() != null && g.getUsers().stream().anyMatch(u -> u.getId() != null && u.getId().equals(userId));
                    if (!isMember) {
                        return ResponseEntity.ok(List.of());
                    }
                    List<Message> messages = messageRepository.findByGroup(g);
                    return ResponseEntity.ok(messages);
                } else {
                    return ResponseEntity.ok(List.of());
                }
            } else if (receiverId != null && userId != null) {
                if (userRepository.findById(userId).isEmpty() || userRepository.findById(receiverId).isEmpty()) {
                    return ResponseEntity.ok(List.of());
                }
                List<Message> messages = messageRepository.findChatHistory(userId, receiverId);
                return ResponseEntity.ok(messages);
            } else {
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @PostMapping("/send")
    public Message sendMessage(@RequestBody Message message, @RequestParam(required = false) Long senderId,
            @RequestParam(required = false) Long receiverId, @RequestParam(required = false) Long groupId) {
        // Asignar sender y receiver correctamente usando los IDs si están presentes
        if (message.getSender() == null && senderId != null) {
            message.setSender(userRepository.findById(senderId).orElse(null));
        }
        if (message.getReceiver() == null && receiverId != null) {
            message.setReceiver(userRepository.findById(receiverId).orElse(null));
        }
        
        // Manejar mensajes de grupo
        if (groupId != null) {
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isPresent()) {
                message.setGroup(groupOpt.get());
            }
        } else if (message.getGroup() != null && message.getGroup().getId() != null) {
            message.setGroup(groupRepository.findById(message.getGroup().getId()).orElse(null));
        }
        
        // Si el remitente está deshabilitado, no persistir ni difundir
        if (isSenderDisabled(message)) {
            return message;
        }

        message.setTimestamp(LocalDateTime.now());
        Message savedMessage = messageRepository.save(message);
        
        // Enviar mensaje a través de WebSocket
        if (savedMessage.getGroup() != null) {
            // Mensaje de grupo - solo enviar al canal del grupo
            messagingTemplate.convertAndSend("/topic/group/" + savedMessage.getGroup().getId(), savedMessage);
        } else if (savedMessage.getReceiver() != null) {
            // Mensaje individual - enviar a ambos usuarios
            messagingTemplate.convertAndSendToUser(
                    savedMessage.getReceiver().getUsername(),
                    "/queue/messages",
                    savedMessage);
            // También enviar al remitente para sincronización
            if (savedMessage.getSender() != null) {
                messagingTemplate.convertAndSendToUser(
                        savedMessage.getSender().getUsername(),
                        "/queue/messages",
                        savedMessage);
            }
        }
        
        return savedMessage;
    }

    // Helper para comprobar si el remitente está deshabilitado
    private boolean isSenderDisabled(Message message) {
        try {
            User sender = message.getSender();
            if (sender == null) return false;
            // Prioridad por ID
            if (sender.getId() != null) {
                Optional<User> s = userRepository.findById(sender.getId());
                if (s.isPresent()) {
                    return !Boolean.TRUE.equals(s.get().isEnabled());
                }
            }
            // Fallback por username
            if (sender.getUsername() != null) {
                Optional<User> s = userRepository.findByUsername(sender.getUsername());
                if (s.isPresent()) {
                    return !Boolean.TRUE.equals(s.get().isEnabled());
                }
            }
        } catch (Exception e) {
            // Si ocurre un error al validar, permitir (no bloquear) para evitar falsos negativos
        }
        return false;
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

    @GetMapping("/group/unread-counts")
    public ResponseEntity<Map<Long, Integer>> getGroupUnreadCounts(@RequestParam Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(new HashMap<>());
            }
            // Iterate all groups to check membership (simple approach)
            List<Group> allGroups = groupRepository.findAll();
            Map<Long, Integer> counts = new HashMap<>();
            
            for (Group g : allGroups) {
                if (g.getUsers() != null && g.getUsers().stream().anyMatch(u -> u.getId().equals(userId))) {
                    // User is member
                    Optional<com.chatroom.entity.GroupReadStatus> statusOpt = groupReadStatusRepository.findByUserIdAndGroupId(userId, g.getId());
                    Long lastReadId = statusOpt.map(com.chatroom.entity.GroupReadStatus::getLastReadMessageId).orElse(0L);
                    
                    int count = messageRepository.countGroupUnreadMessages(g.getId(), lastReadId, userId);
                    counts.put(g.getId(), count);
                }
            }
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new HashMap<>());
        }
    }

    @PostMapping("/group/mark-read")
    public ResponseEntity<String> markGroupMessagesAsRead(@RequestParam Long userId, @RequestParam Long groupId) {
        try {
            // Check if user is member
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) return ResponseEntity.badRequest().body("Group not found");
            Group g = groupOpt.get();
            if (g.getUsers().stream().noneMatch(u -> u.getId().equals(userId))) {
                return ResponseEntity.badRequest().body("User not in group");
            }
            
            // Find last message ID
            Long lastMsgId = messageRepository.findLastMessageIdInGroup(groupId);
            if (lastMsgId == null) lastMsgId = 0L;
            
            Optional<com.chatroom.entity.GroupReadStatus> statusOpt = groupReadStatusRepository.findByUserIdAndGroupId(userId, groupId);
            com.chatroom.entity.GroupReadStatus status;
            if (statusOpt.isPresent()) {
                status = statusOpt.get();
            } else {
                status = new com.chatroom.entity.GroupReadStatus();
                status.setUserId(userId);
                status.setGroupId(groupId);
            }
            status.setLastReadMessageId(lastMsgId);
            groupReadStatusRepository.save(status);
            
            return ResponseEntity.ok("Marked as read");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error");
        }
    }
}