package com.javaisland.bank_backend.audit.service;

import com.javaisland.bank_backend.audit.dto.AuditLogDto;
import com.javaisland.bank_backend.audit.model.AuditLog;
import com.javaisland.bank_backend.audit.repository.AuditLogRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(String entityType, Long entityId, String action, String performedBy, String details) {
        String resolvedPerformer = resolvePerformer(performedBy);
        User resolvedUser = resolvePerformerUser();
        AuditLog entry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedBy(resolvedPerformer)
                .performedByUser(resolvedUser)
                .details(details)
                .build();
        auditLogRepository.save(entry);
        log.info("AUDIT: {} {} {} by {} — {}", entityType, entityId, action, resolvedPerformer, details);
    }

    private String resolvePerformer(String fallback) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String keycloakId = jwt.getSubject();
                return userRepository.findByKeycloakId(keycloakId)
                        .map(u -> u.getFirstName() + " " + u.getLastName())
                        .orElse(fallback);
            }
        } catch (Exception e) {
            log.debug("Cannot resolve performer from SecurityContext: {}", e.getMessage());
        }
        return fallback;
    }

    private User resolvePerformerUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String keycloakId = jwt.getSubject();
                return userRepository.findByKeycloakId(keycloakId).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Cannot resolve performer user from SecurityContext: {}", e.getMessage());
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAll() {
        return auditLogRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByAction(String action) {
        return auditLogRepository.findByActionOrderByPerformedAtDesc(action).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByDateRange(OffsetDateTime from, OffsetDateTime to) {
        return auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(from, to).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getByActionAndDateRange(String action, OffsetDateTime from, OffsetDateTime to) {
        return auditLogRepository.findByActionAndPerformedAtBetweenOrderByPerformedAtDesc(action, from, to).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getRecent(int days) {
        return auditLogRepository.findRecentLogs(OffsetDateTime.now().minusDays(days)).stream()
                .map(this::toDto)
                .toList();
    }

    private AuditLogDto toDto(AuditLog entry) {
        User pu = entry.getPerformedByUser();
        return AuditLogDto.builder()
                .id(entry.getId())
                .entityType(entry.getEntityType())
                .entityId(entry.getEntityId())
                .action(entry.getAction())
                .performedBy(entry.getPerformedBy())
                .performedByUserId(pu != null ? pu.getId() : null)
                .performedByUserEmail(pu != null ? pu.getEmail() : null)
                .details(entry.getDetails())
                .performedAt(entry.getPerformedAt())
                .build();
    }
}
