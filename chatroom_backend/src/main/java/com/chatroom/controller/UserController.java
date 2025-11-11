package com.chatroom.controller;

import com.chatroom.entity.User;
import com.chatroom.service.UserService;
import com.chatroom.service.OnlineUserService;
import com.chatroom.repository.GroupRepository;
import com.chatroom.entity.Group;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private OnlineUserService onlineUserService;
    @Autowired
    private GroupRepository groupRepository;

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.register(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        // Encontrar por username para distinguir entre contraseña incorrecta y usuario deshabilitado
        Optional<User> foundOpt = userService.findByUsername(user.getUsername());
        if (foundOpt.isPresent()) {
            User found = foundOpt.get();
            // Cuenta deshabilitada: 403 con mensaje claro
            if (!Boolean.TRUE.equals(found.isEnabled())) {
                return ResponseEntity.status(403).body(Map.of("message", "Cuenta deshabilitada por el administrador"));
            }
            // Contraseña correcta: OK
            if (found.getPassword() != null && found.getPassword().equals(user.getPassword())) {
                return ResponseEntity.ok(found);
            }
            // Contraseña incorrecta: 401 con mensaje
            return ResponseEntity.status(401).body(Map.of("message", "Usuario o contraseña incorrectos"));
        }
        // Usuario inexistente: 401 con mensaje genérico
        return ResponseEntity.status(401).body(Map.of("message", "Usuario o contraseña incorrectos"));
    }

    @GetMapping("/list")
    public List<User> listUsers() {
        return userService.findAll();
    }

    @GetMapping("/groups")
    public List<Group> listGroups(@RequestParam(required = false) Long userId) {
        try {
            if (userId != null) {
                return groupRepository.findByUsers_Id(userId);
            }
            return groupRepository.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            // Return empty list instead of throwing exception
            return new ArrayList<>();
        }
    }

    @GetMapping("/online")
    public Set<User> getOnlineUsers() {
        return onlineUserService.getOnlineUsers();
    }

    @GetMapping("/online/{userId}")
    public ResponseEntity<Boolean> isUserOnline(@PathVariable Long userId) {
        boolean isOnline = onlineUserService.isUserOnline(userId);
        return ResponseEntity.ok(isOnline);
    }

    @GetMapping("/online/count")
    public ResponseEntity<Integer> getOnlineUserCount() {
        int count = onlineUserService.getOnlineUserCount();
        return ResponseEntity.ok(count);
    }
}