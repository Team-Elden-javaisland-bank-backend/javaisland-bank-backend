package com.javaisland.bank_backend.user.controller;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.LimitChangeRequest;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.repository.LimitChangeRequestRepository;
import com.javaisland.bank_backend.card.model.Card;
import com.javaisland.bank_backend.card.repository.CardRepository;
import com.javaisland.bank_backend.user.dto.CustomerRequestDto;
import com.javaisland.bank_backend.user.dto.PasswordChangeRequestDto;
import com.javaisland.bank_backend.user.model.PasswordChangeRequest;
import com.javaisland.bank_backend.user.repository.PasswordChangeRequestRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
public class CustomerRequestController {

    private final PasswordChangeRequestRepository passwordChangeRequestRepository;
    private final LimitChangeRequestRepository limitChangeRequestRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<CustomerRequestDto>> getMyRequests(@AuthenticationPrincipal Jwt jwt) {
        Long userId = userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow().getId();

        List<CustomerRequestDto> all = new ArrayList<>();

        passwordChangeRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(req -> all.add(CustomerRequestDto.builder()
                        .id(req.getId())
                        .type("PASSWORD_CHANGE")
                        .status(req.getStatus())
                        .description("Cambio password richiesto")
                        .createdAt(req.getCreatedAt())
                        .processedAt(req.getProcessedAt())
                        .build()));

        limitChangeRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(req -> all.add(CustomerRequestDto.builder()
                        .id(req.getId())
                        .type("LIMIT_CHANGE")
                        .status(req.getStatus())
                        .description("Modifica limite " + req.getLimitTypeName() + " — richiesto: €" + req.getRequestedAmount())
                        .createdAt(req.getCreatedAt())
                        .processedAt(req.getProcessedAt())
                        .build()));

        accountRepository.findByUserId(userId)
                .forEach(acc -> {
                    if (acc.getStatusId() == 1) {
                        all.add(CustomerRequestDto.builder()
                                .id(acc.getId())
                                .type("ACCOUNT_OPENING")
                                .status("PENDING")
                                .description("Apertura conto in attesa di approvazione — IBAN: " + acc.getAccountNumber())
                                .createdAt(acc.getCreatedAt())
                                .build());
                    } else if (acc.getStatusId() == 3) {
                        if (acc.getClosureRequestedAt() != null) {
                            all.add(CustomerRequestDto.builder()
                                    .id(acc.getId())
                                    .type("ACCOUNT_CLOSURE")
                                    .status("PENDING")
                                    .description("Richiesta di chiusura conto in attesa di approvazione — IBAN: " + acc.getAccountNumber())
                                    .createdAt(acc.getClosureRequestedAt())
                                    .build());
                        } else {
                            all.add(CustomerRequestDto.builder()
                                    .id(acc.getId())
                                    .type("ACCOUNT_FROZEN")
                                    .status("APPROVED")
                                    .description("Conto congelato — IBAN: " + acc.getAccountNumber())
                                    .createdAt(acc.getCreatedAt())
                                    .build());
                        }
                    } else if (acc.getStatusId() == 4) {
                        all.add(CustomerRequestDto.builder()
                                .id(acc.getId())
                                .type("ACCOUNT_CLOSURE")
                                .status("APPROVED")
                                .description("Conto chiuso — IBAN: " + acc.getAccountNumber())
                                .createdAt(acc.getCreatedAt())
                                .processedAt(acc.getClosedAt())
                                .build());
                    } else if (acc.getStatusId() == 2 && acc.getClosureRejectedAt() != null) {
                        all.add(CustomerRequestDto.builder()
                                .id(acc.getId())
                                .type("ACCOUNT_CLOSURE")
                                .status("REJECTED")
                                .description("Richiesta di chiusura conto rifiutata — IBAN: " + acc.getAccountNumber())
                                .createdAt(acc.getClosureRejectedAt())
                                .processedAt(acc.getClosureRejectedAt())
                                .build());
                    }
                });

        all.sort(Comparator.comparing(CustomerRequestDto::getCreatedAt).reversed());
        return ResponseEntity.ok(all);
    }
}
