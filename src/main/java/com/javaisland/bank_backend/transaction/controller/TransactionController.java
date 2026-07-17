package com.javaisland.bank_backend.transaction.controller;

import com.javaisland.bank_backend.common.PageResponseDto;
import com.javaisland.bank_backend.transaction.dto.TransferRequestDto;
import com.javaisland.bank_backend.transaction.dto.TransactionRequestDto;
import com.javaisland.bank_backend.transaction.dto.TransactionResponseDto;
import com.javaisland.bank_backend.transaction.service.TransactionService;
import com.javaisland.bank_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, String>> deposit(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody TransactionRequestDto request) {
        Long userId = getUserId(jwt);
        transactionService.deposit(userId, request);
        return ResponseEntity.ok(Map.of("message", "Deposit completed."));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, String>> withdraw(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody TransactionRequestDto request) {
        Long userId = getUserId(jwt);
        transactionService.withdraw(userId, request);
        return ResponseEntity.ok(Map.of("message", "Withdrawal completed."));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponseDto> transfer(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody TransferRequestDto request) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(transactionService.transfer(userId, request));
    }

    @GetMapping("/recent/{accountNumber}")
    public ResponseEntity<List<TransactionResponseDto>> getLast10(@AuthenticationPrincipal Jwt jwt,
                                                                    @PathVariable String accountNumber) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(transactionService.getLast10Transactions(userId, accountNumber));
    }

    @GetMapping("/all")
    public ResponseEntity<PageResponseDto<TransactionResponseDto>> getAllTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(transactionService.getAllAccountsTransactions(
                userId, start.atStartOfDay(), end.atTime(23, 59, 59), page, size));
    }

    @DeleteMapping("/{transactionId}/cancel")
    public ResponseEntity<Map<String, String>> cancelTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long transactionId) {
        Long userId = getUserId(jwt);
        transactionService.cancelPendingTransaction(userId, transactionId);
        return ResponseEntity.ok(Map.of("message", "Transaction cancelled."));
    }

    private Long getUserId(Jwt jwt) {
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new com.javaisland.bank_backend.exception.ApiBankException(
                        "Utente non trovato.", "USER_NOT_FOUND"))
                .getId();
    }
}
