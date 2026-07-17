package com.javaisland.bank_backend.admin.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AdminDashboardDto(
    long totalCustomers,
    long totalEmployees,
    long totalAccounts,
    long activeAccounts,
    long frozenAccounts,
    long pendingRegistrations,
    long totalTransactions,
    BigDecimal totalBalance
) {}
