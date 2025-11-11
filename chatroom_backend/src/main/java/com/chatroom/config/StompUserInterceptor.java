package com.chatroom.config;

import com.chatroom.entity.User;
import com.chatroom.entity.Group;
import com.chatroom.service.UserService;
import com.chatroom.repository.GroupRepository;
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
    private final GroupRepository groupRepository;

    public StompUserInterceptor(UserService userService, GroupRepository groupRepository) {
        this.userService = userService;
        this.groupRepository = groupRepository;
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
                if (userOpt.isPresent()) {
                    User u = userOpt.get();
                    // Bloquear completamente SEND/SUBSCRIBE si está deshabilitado
                    if (!Boolean.TRUE.equals(u.isEnabled())) {
                        if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                            return null;
                        }
                    }
                    // Validar suscripciones a canales de grupo por membresía
                    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        String destination = accessor.getDestination();
                        if (destination != null && destination.startsWith("/topic/group/")) {
                            try {
                                String idStr = destination.substring("/topic/group/".length());
                                Long groupId = Long.parseLong(idStr);
                                Optional<Group> gOpt = groupRepository.findById(groupId);
                                if (gOpt.isPresent()) {
                                    Group g = gOpt.get();
                                    boolean isMember = g.getUsers() != null && g.getUsers().stream().anyMatch(m -> m.getId() != null && m.getId().equals(u.getId()));
                                    if (!isMember) {
                                        // Usuario no miembro: bloquear suscripción
                                        return null;
                                    }
                                } else {
                                    // Grupo inexistente: bloquear suscripción
                                    return null;
                                }
                            } catch (Exception ignored) {
                                return null;
                            }
                        }
                    }
                }
            }
        }

        return message;
    }
}