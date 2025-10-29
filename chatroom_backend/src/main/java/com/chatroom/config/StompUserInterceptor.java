package com.chatroom.config;

import com.chatroom.entity.User;
import com.chatroom.service.UserService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.Optional;

public class StompUserInterceptor implements ChannelInterceptor {
    private final UserService userService;

    public StompUserInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        // En CONNECT, fijamos el Principal y rechazamos si el usuario está deshabilitado
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String username = accessor.getFirstNativeHeader("username");
            if (username != null && !username.isEmpty()) {
                Optional<User> userOpt = userService.findByUsername(username);
                if (userOpt.isPresent() && Boolean.TRUE.equals(userOpt.get().isEnabled())) {
                    accessor.setUser(new StompPrincipal(username));
                } else {
                    throw new IllegalStateException("Usuario deshabilitado: " + username);
                }
            }
        } else {
            // En SEND/SUBSCRIBE, bloqueamos acciones si el usuario está deshabilitado
            Principal principal = accessor.getUser();
            if (principal != null) {
                Optional<User> userOpt = userService.findByUsername(principal.getName());
                if (userOpt.isPresent() && !Boolean.TRUE.equals(userOpt.get().isEnabled())) {
                    if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        return null; // descartar la acción
                    }
                }
            }
        }

        return message;
    }
}