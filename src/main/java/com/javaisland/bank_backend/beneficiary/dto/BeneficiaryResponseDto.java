package com.javaisland.bank_backend.beneficiary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryResponseDto {
    private Long id;
    private String nickname;
    private String beneficiaryName;
    private String destinationAccountNumber;
    private String holderFirstName;
    private String holderLastName;
    private String profilePictureUrl;
    private OffsetDateTime createdAt;
}
