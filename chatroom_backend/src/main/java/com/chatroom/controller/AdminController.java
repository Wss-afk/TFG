package com.chatroom.controller;

import com.chatroom.entity.User;
import com.chatroom.entity.Group;
import com.chatroom.entity.Role;
import com.chatroom.repository.UserRepository;
import com.chatroom.repository.GroupRepository;
import com.chatroom.dto.GroupMembersUpdateRequest;
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
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private OnlineUserService onlineUserService;

    // Simple guard: check header X-Admin-UserId belongs to SUPER_ADMIN
    private boolean isSuperAdmin(String adminUserIdHeader) {
        try {
            if (adminUserIdHeader == null) return false;
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
        if (forbidden != null) return forbidden;
        List<User> nonSuperAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.SUPER_ADMIN)
                .collect(Collectors.toList());
        return ResponseEntity.ok(nonSuperAdmins);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                        @RequestBody User user) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        // Validación básica: username y password no vacíos (tras trim)
        String username = user.getUsername() == null ? null : user.getUsername().trim();
        String password = user.getPassword() == null ? null : user.getPassword().trim();
        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body("username required");
        }
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body("password required");
        }
        // Forzar rol y habilitación por defecto
        user.setRole(Role.USER);
        user.setEnabled(true);
        // simple uniqueness check
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        user.setUsername(username);
        user.setPassword(password);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                        @PathVariable Long id,
                                        @RequestBody User update) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User u = opt.get();
        String previousUsername = u.getUsername();
        // No permitir modificar el SUPER_ADMIN
        if (u.getRole() == Role.SUPER_ADMIN) {
            return ResponseEntity.badRequest().body("Cannot modify SUPER_ADMIN");
        }
        if (update.getUsername() != null && !update.getUsername().equals(u.getUsername())) {
            String newUsernameTrimmed = update.getUsername() == null ? null : update.getUsername().trim();
            if (newUsernameTrimmed == null || newUsernameTrimmed.isEmpty()) {
                return ResponseEntity.badRequest().body("username required");
            }
            if (userRepository.findByUsername(newUsernameTrimmed).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            u.setUsername(newUsernameTrimmed);
        }
        if (update.getAvatarUrl() != null) u.setAvatarUrl(update.getAvatarUrl());
        if (update.getRole() != null) u.setRole(update.getRole());
        // Permitir cambiar habilitación para usuarios no SUPER_ADMIN
        boolean wasEnabled = Boolean.TRUE.equals(u.isEnabled());
        u.setEnabled(update.isEnabled());
        User saved = userRepository.save(u);

        // Si se deshabilita un usuario que estaba habilitado, avisar al cliente para que desconecte
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
        }
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/users/{id}/password")
    public ResponseEntity<?> changePassword(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("newPassword required");
        }
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
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
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                        @PathVariable Long id) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        Optional<User> opt = userRepository.findById(id);
        if (opt.isPresent() && opt.get().getRole() == Role.SUPER_ADMIN) {
            return ResponseEntity.badRequest().body("Cannot delete SUPER_ADMIN");
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // Groups
    @GetMapping("/groups")
    public ResponseEntity<?> listGroups(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        return ResponseEntity.ok(groupRepository.findAll());
    }

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                         @RequestBody Group group) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        return ResponseEntity.ok(groupRepository.save(group));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<?> updateGroup(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                         @PathVariable Long id,
                                         @RequestBody Group update) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        Optional<Group> opt = groupRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Group g = opt.get();
        if (update.getName() != null) g.setName(update.getName());
        return ResponseEntity.ok(groupRepository.save(g));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<?> deleteGroup(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                         @PathVariable Long id) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        if (!groupRepository.existsById(id)) return ResponseEntity.notFound().build();
        groupRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/groups/{id}/members")
    public ResponseEntity<?> updateGroupMembers(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                                @PathVariable Long id,
                                                @RequestBody GroupMembersUpdateRequest request) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        Optional<Group> opt = groupRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Group g = opt.get();
        List<Long> userIds = request.getUserIds();
        if (userIds == null) userIds = Collections.emptyList();
        Set<User> members = new HashSet<>();
        for (Long uid : userIds) {
            userRepository.findById(uid).ifPresent(members::add);
        }
        g.setUsers(members);
        return ResponseEntity.ok(groupRepository.save(g));
    }
}