package com.javaisland.bank_backend.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OpenAccountRequestDto {

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @NotNull(message = "Initial amount is required")
    @DecimalMin(value = "0.01", message = "Initial amount must be greater than zero")
    private BigDecimal initialAmount;
}
