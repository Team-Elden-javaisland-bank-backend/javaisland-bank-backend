package com.javaisland.bank_backend.admin.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EmployeeListItemDto(
    Long userId,
    String username,
    String firstName,
    String lastName,
    String email,
    String status,
    LocalDateTime createdAt
) {}
