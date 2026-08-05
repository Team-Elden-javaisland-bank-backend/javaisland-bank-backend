package com.javaisland.bank_backend.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OpenAccountRequestDto {

    @NotBlank(message = "Source account is required")
    private String sourceAccountNumber;

    @DecimalMin(value = "0.01", message = "Initial amount must be greater than 0")
    private BigDecimal initialAmount;
}
