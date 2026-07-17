package com.javaisland.bank_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PinSetupRequestDto {

    @NotBlank(message = "Il PIN è obbligatorio.")
    @Pattern(regexp = "^\\d{4}$", message = "Il PIN deve essere di 4 cifre.")
    private String pin;
}
