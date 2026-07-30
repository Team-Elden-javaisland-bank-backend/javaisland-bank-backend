package com.javaisland.bank_backend.audit.dto;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record AuditLogDto(
    Long id,
    String entityType,
    Long entityId,
    String action,
    String performedBy,
    Long performedByUserId,
    String performedByUserEmail,
    String details,
    OffsetDateTime performedAt
) {}
