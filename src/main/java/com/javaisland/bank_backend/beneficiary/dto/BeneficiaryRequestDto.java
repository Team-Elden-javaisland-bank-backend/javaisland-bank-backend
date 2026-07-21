package com.javaisland.bank_backend.beneficiary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BeneficiaryRequestDto {

    @NotBlank(message = "Il nickname è obbligatorio")
    @Size(max = 100)
    private String nickname;

    @NotBlank(message = "Il numero del conto di destinazione è obbligatorio")
    private String destinationAccountNumber;
}
