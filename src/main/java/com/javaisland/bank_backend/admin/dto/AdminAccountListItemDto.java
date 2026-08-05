package com.javaisland.bank_backend.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class AdminAccountListItemDto {
    private String accountNumber;
    private BigDecimal balance;
    private Integer statusId;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String profilePictureUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime closedAt;
}
