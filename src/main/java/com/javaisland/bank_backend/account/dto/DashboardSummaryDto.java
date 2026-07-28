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
public class DashboardSummaryDto {
    private BigDecimal totalCurrentBalance;
    private BigDecimal totalPreviousMonthBalance;
    private BigDecimal balanceChangeAbsolute;
    private BigDecimal balanceChangePercentage;
}
