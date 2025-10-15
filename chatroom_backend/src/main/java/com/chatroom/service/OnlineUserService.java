package com.chatroom.service;

import com.chatroom.entity.User;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

@Service
public class OnlineUserService {
    // 存储在线用户：sessionId -> User
    private final Map<String, User> onlineUsers = new ConcurrentHashMap<>();
    // 存储用户的会话ID：userId -> sessionId
    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();

    /**
     * 用户上线
     */
    public void userOnline(String sessionId, User user) {
        // 如果用户已经在线，先移除旧的会话
        if (userSessions.containsKey(user.getId())) {
            String oldSessionId = userSessions.get(user.getId());
            onlineUsers.remove(oldSessionId);
        }
        
        onlineUsers.put(sessionId, user);
        userSessions.put(user.getId(), sessionId);
        System.out.println("用户上线: " + user.getUsername() + ", sessionId: " + sessionId);
    }

    /**
     * 用户下线
     */
    public void userOffline(String sessionId) {
        User user = onlineUsers.remove(sessionId);
        if (user != null) {
            userSessions.remove(user.getId());
            System.out.println("用户下线: " + user.getUsername() + ", sessionId: " + sessionId);
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId);
    }

    /**
     * 获取所有在线用户
     */
    public Set<User> getOnlineUsers() {
        return new HashSet<>(onlineUsers.values());
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    /**
     * 根据用户ID获取会话ID
     */
    public String getSessionId(Long userId) {
        return userSessions.get(userId);
    }

    /**
     * 根据会话ID获取用户
     */
    public User getUserBySessionId(String sessionId) {
        return onlineUsers.get(sessionId);
    }
}