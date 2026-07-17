package com.javaisland.bank_backend.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDto {
    private BigDecimal monthlyExpenses;
    private BigDecimal monthlyIncome;
    private Long movementCount;
    private BigDecimal balanceChangePercentage;
}
