package com.javaisland.bank_backend.savedbeneficiary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedBeneficiaryResponseDto {
    private Integer id;
    private String beneficiaryName;
    private String accountNumber;
}
