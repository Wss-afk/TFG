package com.chatroom.controller;

import com.chatroom.entity.User;
import com.chatroom.service.UserService;
import com.chatroom.service.OnlineUserService;
import com.chatroom.repository.GroupRepository;
import com.chatroom.entity.Group;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<User> login(@RequestBody User user) {
        Optional<User> userOpt = userService.login(user.getUsername(), user.getPassword());
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get());
        } else {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/list")
    public List<User> listUsers() {
        return userService.findAll();
    }

    @GetMapping("/groups")
    public List<Group> listGroups() {
        try {
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