package com.chatroom.controller;

import com.chatroom.entity.User;
import com.chatroom.entity.Group;
import com.chatroom.entity.Role;
import com.chatroom.repository.UserRepository;
import com.chatroom.repository.GroupRepository;
import com.chatroom.dto.GroupMembersUpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

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
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                        @RequestBody User user) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        if (user.getRole() == null) user.setRole(Role.USER);
        // simple uniqueness check
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
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
        if (update.getUsername() != null && !update.getUsername().equals(u.getUsername())) {
            if (userRepository.findByUsername(update.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            u.setUsername(update.getUsername());
        }
        if (update.getAvatarUrl() != null) u.setAvatarUrl(update.getAvatarUrl());
        if (update.getRole() != null) u.setRole(update.getRole());
        return ResponseEntity.ok(userRepository.save(u));
    }

    @PatchMapping("/users/{id}/password")
    public ResponseEntity<?> changePassword(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, String> body) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("newPassword required");
        }
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User u = opt.get();
        u.setPassword(newPassword);
        return ResponseEntity.ok(userRepository.save(u));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
                                        @PathVariable Long id) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
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