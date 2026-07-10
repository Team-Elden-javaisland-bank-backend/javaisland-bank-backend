package com.javaisland.bank_backend.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequestDto {

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    private String destinationAccountNumber;

    private Long beneficiaryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    private String description;
}
