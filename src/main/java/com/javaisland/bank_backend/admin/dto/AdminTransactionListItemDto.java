package com.javaisland.bank_backend.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class AdminTransactionListItemDto {
    private Long id;
    private BigDecimal amount;
    private Integer typeId;
    private Integer statusId;
    private String typeName;
    private String statusName;
    private String description;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private OffsetDateTime createdAt;
}
