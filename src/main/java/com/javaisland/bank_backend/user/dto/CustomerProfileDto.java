package com.javaisland.bank_backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileDto {

    // User
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;
    private String profession;
    private String gender;
    private String fiscalCode;
    private String phone;
    private String residence;
    private String birthPlace;
    private String birthProvince;
    private String userStatus;
    private LocalDateTime registeredAt;

    // Counts
    private int totalAccounts;
    private int activeAccounts;
    private int totalCards;
    private int activeCards;

    // Account details
    private List<AccountSummaryDto> accounts;

    // Card details
    private List<CardSummaryDto> cards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountSummaryDto {
        private String accountNumber;
        private BigDecimal balance;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardSummaryDto {
        private Long id;
        private String maskedCardNumber;
        private String holderName;
        private LocalDate expirationDate;
        private String cardType;
        private String cardStatus;
    }
}
