package com.javaisland.bank_backend.beneficiary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryResponseDto {
    private Long id;
    private String nickname;
    private String destinationAccountNumber;
    private LocalDateTime createdAt;
}
