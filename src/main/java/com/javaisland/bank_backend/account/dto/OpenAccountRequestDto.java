package com.javaisland.bank_backend.account.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OpenAccountRequestDto {

    private String sourceAccountNumber;

    @DecimalMin(value = "1.00", message = "L'importo minimo da trasferire è 1 euro")
    private BigDecimal initialAmount;
}
