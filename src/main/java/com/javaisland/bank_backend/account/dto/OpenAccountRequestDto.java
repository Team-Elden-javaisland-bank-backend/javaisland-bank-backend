package com.javaisland.bank_backend.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OpenAccountRequestDto {

    @NotBlank(message = "Il conto sorgente è obbligatorio")
    private String sourceAccountNumber;

    @DecimalMin(value = "0.01", message = "L'importo iniziale deve essere maggiore di 0")
    private BigDecimal initialAmount;
}
