package com.javaisland.bank_backend.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class AdminCustomerListItemDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String status;
    private String profilePictureUrl;
    private int accountCount;
    private BigDecimal totalBalance;
    private OffsetDateTime createdAt;
}
