package com.javaisland.bank_backend.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequestDto {
    private Long id;
    private String type;
    private String status;
    private String description;
    private Long userId;
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String accountNumber;
    private String limitTypeName;
    private BigDecimal requestedAmount;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
