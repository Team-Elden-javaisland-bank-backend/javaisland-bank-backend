package com.javaisland.bank_backend.user.controller;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.dto.CustomerProfileDto;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
public class CustomerProfileController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CardService cardService;

    @GetMapping
    public ResponseEntity<CustomerProfileDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        User user = userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        var accounts = accountRepository.findByUserId(user.getId());
        int totalAccounts = accounts.size();
        int activeAccounts = (int) accounts.stream()
                .filter(a -> a.getStatusId() == AccountStatus.ACTIVE)
                .count();

        var cards = cardService.getCardsByUserId(user.getId());
        int totalCards = cards.size();
        int activeCards = (int) cards.stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .count();

        var accountSummaries = accounts.stream()
                .map(a -> CustomerProfileDto.AccountSummaryDto.builder()
                        .accountNumber(a.getAccountNumber())
                        .balance(a.getBalance())
                        .status(getStatusName(a.getStatusId()))
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        var cardSummaries = cards.stream()
                .map(c -> CustomerProfileDto.CardSummaryDto.builder()
                        .id(c.getId())
                        .maskedCardNumber(c.getMaskedCardNumber())
                        .holderName(c.getHolderName())
                        .expirationDate(c.getExpirationDate())
                        .cardType(c.getCardType())
                        .cardStatus(c.getStatus())
                        .build())
                .toList();

        return ResponseEntity.ok(CustomerProfileDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .profession(user.getProfession())
                .gender(user.getGender())
                .fiscalCode(user.getFiscalCode())
                .phone(user.getPhone())
                .residence(user.getResidence())
                .birthPlace(user.getBirthPlace())
                .birthProvince(user.getBirthProvince())
                .userStatus(user.getStatus().getUserStatus())
                .registeredAt(user.getCreatedAt())
                .totalAccounts(totalAccounts)
                .activeAccounts(activeAccounts)
                .totalCards(totalCards)
                .activeCards(activeCards)
                .accounts(accountSummaries)
                .cards(cardSummaries)
                .build());
    }

    private String getStatusName(Integer statusId) {
        if (statusId == null) return "SCONOSCIUTO";
        return switch (statusId) {
            case 1 -> "INATTIVO";
            case 2 -> "ATTIVO";
            case 3 -> "CONGELATO";
            case 4 -> "CHIUSO";
            default -> "SCONOSCIUTO";
        };
    }
}
