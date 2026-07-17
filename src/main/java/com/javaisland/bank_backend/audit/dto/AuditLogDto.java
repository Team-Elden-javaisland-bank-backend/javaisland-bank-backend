package com.javaisland.bank_backend.audit.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuditLogDto(
    Long id,
    String entityType,
    Long entityId,
    String action,
    String performedBy,
    Long performedByUserId,
    String details,
    LocalDateTime performedAt
) {}
