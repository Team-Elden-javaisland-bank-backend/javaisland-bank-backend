package com.javaisland.bank_backend.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CloseAccountRequestDto {

    @NotBlank(message = "Account number is required")
    private String accountNumber;
}
