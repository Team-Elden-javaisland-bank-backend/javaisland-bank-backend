package com.javaisland.bank_backend.account;

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
public class AccountResponseDto {
    private String accountNumber;
    private BigDecimal balance;
    private Integer statusId;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
