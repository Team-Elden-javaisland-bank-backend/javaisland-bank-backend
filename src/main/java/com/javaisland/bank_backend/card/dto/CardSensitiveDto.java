package com.javaisland.bank_backend.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSensitiveDto {
    private String cardNumber;
    private String cvv;
}
