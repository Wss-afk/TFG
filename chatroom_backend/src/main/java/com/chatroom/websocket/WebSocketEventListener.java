package com.chatroom.websocket;

import com.chatroom.entity.User;
import com.chatroom.service.OnlineUserService;
import com.chatroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

@Component
public class WebSocketEventListener {

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 优先从Principal获取用户名（由拦截器在CONNECT时设置），回退到原生header
        Principal principal = headerAccessor.getUser();
        String username = principal != null ? principal.getName() : headerAccessor.getFirstNativeHeader("username");
        if (username != null) {
            try {
                Optional<User> userOpt = userService.findByUsername(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    // 用户上线
                    onlineUserService.userOnline(sessionId, user);
                    
                    // 广播在线用户列表更新
                    broadcastOnlineUsers();
                }
            } catch (Exception e) {
                System.err.println("处理用户连接时出错: " + e.getMessage());
            }
        }
        
        System.out.println("WebSocket连接建立: sessionId = " + sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 用户下线
        onlineUserService.userOffline(sessionId);
        
        // 广播在线用户列表更新
        broadcastOnlineUsers();
        
        System.out.println("WebSocket连接断开: sessionId = " + sessionId);
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        System.out.println("WebSocket订阅: sessionId = " + sessionId + ", destination = " + destination);
    }

    /**
     * 广播在线用户列表
     */
    private void broadcastOnlineUsers() {
        try {
            Set<User> onlineUsers = onlineUserService.getOnlineUsers();
            messagingTemplate.convertAndSend("/topic/online-users", onlineUsers);
        } catch (Exception e) {
            System.err.println("广播在线用户列表时出错: " + e.getMessage());
        }
    }
}