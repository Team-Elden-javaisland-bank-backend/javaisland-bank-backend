package com.javaisland.bank_backend.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransferRequestDto {

    @NotBlank(message = "Il numero del conto sorgente è obbligatorio")
    private String sourceAccountNumber;

    private String destinationAccountNumber;

    private Long beneficiaryId;

    @NotNull(message = "L'importo è obbligatorio")
    @DecimalMin(value = "0.01", message = "L'importo deve essere positivo")
    private BigDecimal amount;

    private String description;

    private Boolean isInstant;

    private LocalDate scheduledDate;
}
