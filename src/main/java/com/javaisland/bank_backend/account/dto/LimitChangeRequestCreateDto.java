package com.javaisland.bank_backend.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitChangeRequestCreateDto {

    @NotBlank(message = "IBAN conto obbligatorio")
    private String accountNumber;

    @NotBlank(message = "Tipo limite obbligatorio")
    private String limitType;

    @NotNull(message = "Importo richiesto obbligatorio")
    @DecimalMin(value = "0.01", message = "Importo deve essere maggiore di 0")
    private BigDecimal requestedAmount;
}
