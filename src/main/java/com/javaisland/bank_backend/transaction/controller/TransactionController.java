package com.javaisland.bank_backend.transaction.controller;

import com.javaisland.bank_backend.common.PageResponseDto;
import com.javaisland.bank_backend.transaction.dto.TransferRequestDto;
import com.javaisland.bank_backend.transaction.dto.TransactionRequestDto;
import com.javaisland.bank_backend.transaction.dto.TransactionResponseDto;
import com.javaisland.bank_backend.transaction.service.TransactionService;
import com.javaisland.bank_backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
@Tag(name = "Customer Transactions", description = "Deposit, withdraw, transfer and transaction history")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds", description = "Deposits the specified amount into the given account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deposit completed"),
        @ApiResponse(responseCode = "400", description = "Validation error or business rule violation")
    })
    public ResponseEntity<TransactionResponseDto> deposit(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody TransactionRequestDto request) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(transactionService.deposit(userId, request));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds", description = "Withdraws the specified amount from the given account")
    public ResponseEntity<TransactionResponseDto> withdraw(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody TransactionRequestDto request) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(transactionService.withdraw(userId, request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds", description = "Transfers funds between accounts (instant or scheduled)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transfer completed"),
        @ApiResponse(responseCode = "400", description = "Validation error or insufficient funds")
    })
    public ResponseEntity<TransactionResponseDto> transfer(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody TransferRequestDto request) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(transactionService.transfer(userId, request));
    }

    @GetMapping("/recent/{accountNumber}")
    @Operation(summary = "Get recent transactions", description = "Returns the last 10 transactions for the given account")
    public ResponseEntity<List<TransactionResponseDto>> getLast10(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable String accountNumber) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(transactionService.getLast10Transactions(userId, accountNumber));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all transactions", description = "Returns paginated transactions within a date range for all user accounts")
    public ResponseEntity<PageResponseDto<TransactionResponseDto>> getAllTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(transactionService.getAllAccountsTransactions(
                userId, start.atStartOfDay(ZoneId.of("Europe/Rome")).toOffsetDateTime(), end.atTime(23, 59, 59).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime(), page, size, accountNumber));
    }

    @GetMapping("/scheduled")
    @Operation(summary = "Get scheduled transfers", description = "Returns the scheduled transfers (bonifici programmati) for the logged user")
    public ResponseEntity<List<TransactionResponseDto>> getScheduledTransfers(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(transactionService.getScheduledTransfers(userId));
    }

    @DeleteMapping("/{transactionId}/cancel")
    @Operation(summary = "Cancel pending transaction", description = "Cancels a pending scheduled transaction")
    public ResponseEntity<Void> cancelTransaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long transactionId) {
        Long userId = getUserId(jwt);
        transactionService.cancelPendingTransaction(userId, transactionId);
        return ResponseEntity.ok().build();
    }

    private Long getUserId(Jwt jwt) {
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new com.javaisland.bank_backend.exception.ApiBankException(
                        "USER_NOT_FOUND", "USER_NOT_FOUND"))
                .getId();
    }
}
