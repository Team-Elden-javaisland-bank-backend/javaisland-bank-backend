package com.javaisland.bank_backend.transaction.dto;

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
public class TransactionResponseDto {
    private Long id;
    private BigDecimal amount;
    private Integer typeId;
    private Integer statusId;
    private String typeName;
    private String statusName;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime scheduledDate;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String sourceUserName;
    private String destinationUserName;
    private BigDecimal sourceBalanceAfter;
    private BigDecimal destBalanceAfter;
}
