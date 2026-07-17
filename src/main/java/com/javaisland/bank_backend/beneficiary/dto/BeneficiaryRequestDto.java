package com.javaisland.bank_backend.beneficiary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BeneficiaryRequestDto {

    @NotBlank(message = "Nickname is required")
    @Size(max = 100)
    private String nickname;

    @NotBlank(message = "Destination account number is required")
    private String destinationAccountNumber;

    @Size(max = 200)
    private String destinationHolderName;
}
