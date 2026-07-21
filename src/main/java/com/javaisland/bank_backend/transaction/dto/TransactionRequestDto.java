package com.javaisland.bank_backend.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransactionRequestDto {

    @NotBlank(message = "Il numero del conto è obbligatorio")
    private String accountNumber;

    @NotNull(message = "L'importo è obbligatorio")
    @DecimalMin(value = "0.01", message = "L'importo deve essere positivo")
    private BigDecimal amount;
}
