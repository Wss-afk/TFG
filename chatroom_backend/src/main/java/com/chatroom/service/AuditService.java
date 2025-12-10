package com.chatroom.service;

import com.chatroom.entity.AuditLog;
import com.chatroom.entity.Role;
import com.chatroom.entity.User;
import com.chatroom.repository.AuditLogRepository;
import com.chatroom.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
public class AuditService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public AuditLog logAdminAction(String adminUserIdHeader,
                                   String action,
                                   String targetType,
                                   Long targetId,
                                   String targetName,
                                   boolean success,
                                   Integer statusCode,
                                   Map<String, Object> details,
                                   String ip,
                                   String userAgent) {
        AuditLog log = new AuditLog();
        log.setTimestamp(Instant.now());

        // Resolve actor
        try {
            if (adminUserIdHeader != null) {
                Long adminId = Long.parseLong(adminUserIdHeader);
                Optional<User> adminOpt = userRepository.findById(adminId);
                if (adminOpt.isPresent()) {
                    User admin = adminOpt.get();
                    log.setActorId(admin.getId());
                    log.setActorUsername(admin.getUsername());
                    log.setActorRole(admin.getRole());
                }
            }
        } catch (Exception ignored) {}

        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setSuccess(success);
        log.setStatusCode(statusCode);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        try {
            log.setDetails(details == null ? null : objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            log.setDetails(details == null ? null : details.toString());
        }

        return auditLogRepository.save(log);
    }

    public void clearAllLogs() {
        auditLogRepository.deleteAll();
    }

    public Specification<AuditLog> buildSpec(Instant from,
                                              Instant to,
                                              Long actorId,
                                              String action,
                                              String targetType,
                                              Long targetId,
                                              Boolean success) {
        Specification<AuditLog> spec = Specification.where(null);
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }
        if (actorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorId"), actorId));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (targetType != null && !targetType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetType"), targetType));
        }
        if (targetId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetId"), targetId));
        }
        if (success != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("success"), success));
        }
        return spec;
    }
}