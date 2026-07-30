package com.javaisland.bank_backend.admin.dto;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record EmployeeListItemDto(
    Long userId,
    String username,
    String firstName,
    String lastName,
    String email,
    String status,
    String profilePictureUrl,
    OffsetDateTime createdAt
) {}
