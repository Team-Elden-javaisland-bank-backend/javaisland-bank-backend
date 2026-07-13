package com.javaisland.bank_backend.account.dto;

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
public class AccountLimitResponseDto {
    private Long id;
    private String limitType;
    private BigDecimal maxAmount;
    private LocalDateTime updatedAt;
}
