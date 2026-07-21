package com.javaisland.bank_backend.savedbeneficiary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedBeneficiaryRequestDto {

    @NotBlank(message = "Il nome del beneficiario è obbligatorio")
    @Size(max = 150, message = "Il nome non può superare i 150 caratteri")
    private String beneficiaryName;

    @NotBlank(message = "Il numero di conto è obbligatorio")
    @Size(max = 50, message = "Il numero di conto non può superare i 50 caratteri")
    private String accountNumber;
}
