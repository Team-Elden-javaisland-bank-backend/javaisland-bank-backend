package com.javaisland.bank_backend.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDto {
    private String accountNumber;
    private BigDecimal balance;
    private Integer statusId;
    private Long profileId;
    private String profileFirstName;
    private String profileLastName;
    private String profilePictureUrl;
    private Integer userStatusId;
    private BigDecimal initialAmount;
    private Boolean isLimitsConfigured;
    private OffsetDateTime createdAt;
    private OffsetDateTime closedAt;
    private OffsetDateTime closureRequestedAt;
}
