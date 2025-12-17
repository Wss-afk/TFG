package com.chatroom.controller;

import com.chatroom.repository.MessageRepository;
import com.chatroom.repository.EventRepository;
import com.chatroom.entity.User;
import com.chatroom.entity.Group;
import com.chatroom.entity.Event;
import com.chatroom.entity.Role;
import com.chatroom.repository.UserRepository;
import com.chatroom.repository.GroupRepository;
import com.chatroom.dto.GroupMembersUpdateRequest;
import com.chatroom.dto.CreateGroupRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import com.chatroom.service.OnlineUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private com.chatroom.service.AuditService auditService;

    // Simple guard: check header X-Admin-UserId belongs to SUPER_ADMIN
    private boolean isSuperAdmin(String adminUserIdHeader) {
        try {
            if (adminUserIdHeader == null)
                return false;
            Long adminId = Long.parseLong(adminUserIdHeader);
            Optional<User> adminOpt = userRepository.findById(adminId);
            return adminOpt.isPresent() && adminOpt.get().getRole() == Role.SUPER_ADMIN;
        } catch (Exception e) {
            return false;
        }
    }

    private ResponseEntity<?> forbidIfNotSuperAdmin(String adminUserIdHeader) {
        if (!isSuperAdmin(adminUserIdHeader)) {
            return ResponseEntity.status(403).body("Forbidden: SUPER_ADMIN required");
        }
        return null;
    }

    // Users
    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        List<User> nonSuperAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.SUPER_ADMIN)
                .collect(Collectors.toList());
        return ResponseEntity.ok(nonSuperAdmins);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
            @RequestBody User user) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        // Validación básica: username y password no vacíos (tras trim)
        String username = user.getUsername() == null ? null : user.getUsername().trim();
        String password = user.getPassword() == null ? null : user.getPassword().trim();
        if (username == null || username.isEmpty()) {
            auditService.logAdminAction(adminUserId, "USER_CREATE", "USER", null, user.getUsername(), false, 400,
                    Map.of("error", "username required"), null, null);
            return ResponseEntity.badRequest().body("username required");
        }
        if (password == null || password.isEmpty()) {
            auditService.logAdminAction(adminUserId, "USER_CREATE", "USER", null, user.getUsername(), false, 400,
                    Map.of("error", "password required"), null, null);
            return ResponseEntity.badRequest().body("password required");
        }
        // Forzar rol y habilitación por defecto
        user.setRole(Role.USER);
        user.setEnabled(true);
        // simple uniqueness check
        if (userRepository.findByUsername(username).isPresent()) {
            auditService.logAdminAction(adminUserId, "USER_CREATE", "USER", null, username, false, 400,
                    Map.of("error", "Username already exists"), null, null);
            return ResponseEntity.badRequest().body("Username already exists");
        }
        user.setUsername(username);
        user.setPassword(password);
        User saved = userRepository.save(user);
        auditService.logAdminAction(adminUserId, "USER_CREATE", "USER", saved.getId(), saved.getUsername(), true, 200,
                Map.of("role", saved.getRole().name()), null, null);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
            @PathVariable Long id,
            @RequestBody User update) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        User u = opt.get();
        String previousUsername = u.getUsername();
        // No permitir modificar el SUPER_ADMIN
        if (u.getRole() == Role.SUPER_ADMIN) {
            return ResponseEntity.badRequest().body("Cannot modify SUPER_ADMIN");
        }
        if (update.getUsername() != null && !update.getUsername().equals(u.getUsername())) {
            String newUsernameTrimmed = update.getUsername() == null ? null : update.getUsername().trim();
            if (newUsernameTrimmed == null || newUsernameTrimmed.isEmpty()) {
                auditService.logAdminAction(adminUserId, "USER_UPDATE", "USER", u.getId(), u.getUsername(), false, 400,
                        Map.of("error", "username required"), null, null);
                return ResponseEntity.badRequest().body("username required");
            }
            if (userRepository.findByUsername(newUsernameTrimmed).isPresent()) {
                auditService.logAdminAction(adminUserId, "USER_UPDATE", "USER", u.getId(), newUsernameTrimmed, false,
                        400,
                        Map.of("error", "Username already exists"), null, null);
                return ResponseEntity.badRequest().body("Username already exists");
            }
            u.setUsername(newUsernameTrimmed);
        }
        if (update.getAvatarUrl() != null) {
            // Empty string indicates removal of avatar
            u.setAvatarUrl(update.getAvatarUrl().isEmpty() ? null : update.getAvatarUrl());
        }
        if (update.getRole() != null)
            u.setRole(update.getRole());
        // Permitir cambiar habilitación para usuarios no SUPER_ADMIN
        boolean wasEnabled = Boolean.TRUE.equals(u.isEnabled());
        u.setEnabled(update.isEnabled());
        User saved = userRepository.save(u);

        // Notificar actualización de perfil a todos los clientes para refrescar
        // avatares
        try {
            Map<String, Object> updateMsg = new HashMap<>();
            updateMsg.put("action", "user_updated");
            updateMsg.put("userId", saved.getId());
            updateMsg.put("username", saved.getUsername());
            updateMsg.put("avatarUrl", saved.getAvatarUrl());
            messagingTemplate.convertAndSend("/topic/public", updateMsg);
        } catch (Exception e) {
            // best-effort
        }

        // Si se deshabilita un usuario que estaba habilitado, avisar al cliente para
        // que desconecte
        if (wasEnabled && !Boolean.TRUE.equals(saved.isEnabled())) {
            try {
                // Enviar al username actual asociado al usuario
                messagingTemplate.convertAndSendToUser(saved.getUsername(),
                        "/queue/control", Map.of("action", "force_logout", "reason", "account_disabled"));
                // También enviar al username previo por si se cambió durante la actualización
                if (previousUsername != null && !previousUsername.equals(saved.getUsername())) {
                    messagingTemplate.convertAndSendToUser(previousUsername,
                            "/queue/control", Map.of("action", "force_logout", "reason", "account_disabled"));
                }
            } catch (Exception e) {
                // best-effort; no bloquear la operación
            }
            auditService.logAdminAction(adminUserId, "ACCOUNT_DISABLE", "USER", saved.getId(), saved.getUsername(),
                    true, 200,
                    Map.of("enabled", false), null, null);
        }
        auditService.logAdminAction(adminUserId, "USER_UPDATE", "USER", saved.getId(), saved.getUsername(), true, 200,
                Map.of(
                        "usernameChanged", !saved.getUsername().equals(previousUsername),
                        "role", saved.getRole().name(),
                        "enabled", saved.isEnabled()),
                null, null);
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/users/{id}/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            auditService.logAdminAction(adminUserId, "PASSWORD_CHANGE", "USER", id, null, false, 400,
                    Map.of("error", "newPassword required"), null, null);
            return ResponseEntity.badRequest().body("newPassword required");
        }
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        User u = opt.get();
        u.setPassword(newPassword.trim());
        User saved = userRepository.save(u);
        // Forzar logout del usuario si está conectado, ya que su credencial ha cambiado
        try {
            messagingTemplate.convertAndSendToUser(saved.getUsername(),
                    "/queue/control", Map.of("action", "force_logout", "reason", "password_changed"));
        } catch (Exception e) {
            // best-effort; si falla el envío, no bloquear la operación
        }
        auditService.logAdminAction(adminUserId, "PASSWORD_CHANGE", "USER", saved.getId(), saved.getUsername(), true,
                200,
                Map.of("changed", true), null, null);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
            @PathVariable Long id) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Optional<User> opt = userRepository.findById(id);
        if (opt.isPresent() && opt.get().getRole() == Role.SUPER_ADMIN) {
            return ResponseEntity.badRequest().body("Cannot delete SUPER_ADMIN");
        }

        // 1. Remover usuario de todos los grupos
        List<Group> userGroups = groupRepository.findByUsers_Id(id);
        for (Group g : userGroups) {
            g.getUsers().removeIf(u -> u.getId().equals(id));
            groupRepository.save(g);
        }

        // 2. Eliminar mensajes del usuario (enviados o recibidos)
        messageRepository.deleteAllByUserId(id);

        // 3. Limpiar eventos (responsable o creador)
        List<Event> responsibleEvents = eventRepository.findByResponsibles_Id(id);
        for (Event e : responsibleEvents) {
            e.getResponsibles().removeIf(u -> u.getId().equals(id));
            eventRepository.save(e);
        }
        List<Event> createdEvents = eventRepository.findByCreatedBy_Id(id);
        for (Event e : createdEvents) {
            e.setCreatedBy(null);
            eventRepository.save(e);
        }

        // 4. Eliminar usuario
        userRepository.deleteById(id);

        auditService.logAdminAction(adminUserId, "USER_DELETE", "USER", id, opt.map(User::getUsername).orElse(null),
                true, 200,
                Map.of(), null, null);
        return ResponseEntity.ok().build();
    }

    // Groups
    @GetMapping("/groups")
    public ResponseEntity<?> listGroups(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        return ResponseEntity.ok(groupRepository.findAll());
    }

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
            @RequestBody CreateGroupRequest request) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("name required");
        }
        Group g = new Group();
        g.setName(request.getName().trim());
        // Asignar miembros iniciales si se proporcionan
        List<Long> userIds = request.getUserIds();
        if (userIds != null && !userIds.isEmpty()) {
            Set<User> members = new HashSet<>();
            for (Long uid : userIds) {
                userRepository.findById(uid).ifPresent(members::add);
            }
            g.setUsers(members);
        }
        Group saved = groupRepository.save(g);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<?> updateGroup(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
            @PathVariable Long id,
            @RequestBody Group update) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        Optional<Group> opt = groupRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        Group g = opt.get();
        if (update.getName() != null)
            g.setName(update.getName());
        return ResponseEntity.ok(groupRepository.save(g));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<?> deleteGroup(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
            @PathVariable Long id) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        if (!groupRepository.existsById(id))
            return ResponseEntity.notFound().build();
        groupRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/groups/{id}/members")
    public ResponseEntity<?> updateGroupMembers(
            @RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
            @PathVariable Long id,
            @RequestBody GroupMembersUpdateRequest request) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null)
            return forbidden;
        Optional<Group> opt = groupRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();
        Group g = opt.get();
        List<Long> userIds = request.getUserIds();
        if (userIds == null)
            userIds = Collections.emptyList();
        Set<User> members = new HashSet<>();
        for (Long uid : userIds) {
            userRepository.findById(uid).ifPresent(members::add);
        }
        g.setUsers(members);
        Group saved = groupRepository.save(g);
        auditService.logAdminAction(adminUserId, "GROUP_MEMBER_UPDATE", "GROUP", saved.getId(), saved.getName(), true,
                200,
                Map.of("memberCount", saved.getUsers() != null ? saved.getUsers().size() : 0), null, null);
        return ResponseEntity.ok(saved);
    }
}