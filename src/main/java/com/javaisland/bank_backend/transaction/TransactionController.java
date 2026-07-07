package com.javaisland.bank_backend.transaction;

import com.javaisland.bank_backend.common.PageResponseDto;
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

@RestController
@RequestMapping("/api/v1/customer/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@Valid @RequestBody TransactionRequestDto request) {
        transactionService.deposit(request);
        return ResponseEntity.ok("Deposit completed.");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(@Valid @RequestBody TransactionRequestDto request) {
        transactionService.withdraw(request);
        return ResponseEntity.ok("Withdrawal completed.");
    }

    @GetMapping("/recent/{accountNumber}")
    public ResponseEntity<List<TransactionResponseDto>> getLast10(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getLast10Transactions(accountNumber));
    }

    @GetMapping("/all")
    public ResponseEntity<PageResponseDto<TransactionResponseDto>> getAllTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(transactionService.getAllAccountsTransactions(
                keycloakId, start.atStartOfDay(), end.atTime(23, 59, 59), page, size));
    }
}
