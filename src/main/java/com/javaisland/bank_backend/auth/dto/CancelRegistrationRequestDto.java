package com.javaisland.bank_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CancelRegistrationRequestDto {

    @NotNull(message = "L'ID utente è obbligatorio")
    @Schema(example = "0")
    private Long userId;
}
