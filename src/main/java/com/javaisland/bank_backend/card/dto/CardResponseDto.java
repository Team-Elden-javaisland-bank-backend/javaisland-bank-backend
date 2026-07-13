package com.javaisland.bank_backend.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponseDto {
    private Long id;
    private String maskedCardNumber;
    private String holderName;
    private LocalDate expirationDate;
    private String cardType;
    private String status;
    private Long accountId;
    private String accountNumber;
}
