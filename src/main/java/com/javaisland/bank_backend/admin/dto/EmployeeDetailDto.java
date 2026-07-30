package com.javaisland.bank_backend.admin.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Builder
public record EmployeeDetailDto(
    Long userId,
    String firstName,
    String lastName,
    String email,
    LocalDate birthDate,
    String status,
    String profilePictureUrl,
    OffsetDateTime createdAt
) {}
