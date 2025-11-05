package com.chatroom.controller;

import com.chatroom.entity.AuditLog;
import com.chatroom.entity.Role;
import com.chatroom.repository.AuditLogRepository;
import com.chatroom.repository.UserRepository;
import com.chatroom.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/admin")
public class AuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    private boolean isSuperAdmin(String adminUserIdHeader) {
        try {
            if (adminUserIdHeader == null) return false;
            Long adminId = Long.parseLong(adminUserIdHeader);
            return userRepository.findById(adminId)
                    .map(u -> u.getRole() == Role.SUPER_ADMIN)
                    .orElse(false);
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

    @GetMapping("/audit")
    public ResponseEntity<?> listAudit(
            @RequestHeader(value = "X-Admin-UserId", required = false) String adminUserId,
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestParam(value = "actorId", required = false) Long actorId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "targetType", required = false) String targetType,
            @RequestParam(value = "targetId", required = false) Long targetId,
            @RequestParam(value = "success", required = false) Boolean success,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        ResponseEntity<?> forbidden = forbidIfNotSuperAdmin(adminUserId);
        if (forbidden != null) return forbidden;

        Instant from = null, to = null;
        try { if (fromStr != null && !fromStr.isBlank()) from = Instant.parse(fromStr); } catch (DateTimeParseException ignored) {}
        try { if (toStr != null && !toStr.isBlank()) to = Instant.parse(toStr); } catch (DateTimeParseException ignored) {}

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "timestamp"));
        Specification<AuditLog> spec = auditService.buildSpec(from, to, actorId, action, targetType, targetId, success);
        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);
        return ResponseEntity.ok(result);
    }
}