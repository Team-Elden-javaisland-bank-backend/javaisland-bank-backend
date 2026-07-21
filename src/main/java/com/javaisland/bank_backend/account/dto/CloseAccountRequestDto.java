package com.javaisland.bank_backend.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CloseAccountRequestDto {

    @NotBlank(message = "Il numero del conto è obbligatorio")
    private String accountNumber;
}
