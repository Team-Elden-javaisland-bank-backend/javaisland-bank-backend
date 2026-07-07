package com.javaisland.bank_backend.account;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/open")
    public ResponseEntity<AccountResponseDto> openAccount(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody OpenAccountRequestDto request) {
        Account created = accountService.openAdditionalAccount(jwt.getSubject(), request);
        return ResponseEntity.ok(AccountResponseDto.builder()
                .accountNumber(created.getAccountNumber())
                .balance(created.getBalance())
                .statusId(created.getStatusId())
                .createdAt(created.getCreatedAt())
                .build());
    }

    @PostMapping("/closure-request")
    public ResponseEntity<String> requestClosure(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CloseAccountRequestDto request) {
        accountService.requestClosure(jwt.getSubject(), request.getAccountNumber());
        return ResponseEntity.ok("Closure request submitted. An employee will review it shortly.");
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<AccountResponseDto> getBalance(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getBalance(jwt.getSubject(), accountNumber));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDto> getDetail(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountDetail(jwt.getSubject(), accountNumber));
    }
}
