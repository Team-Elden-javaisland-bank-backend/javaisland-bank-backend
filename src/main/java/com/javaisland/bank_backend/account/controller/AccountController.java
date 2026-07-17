package com.javaisland.bank_backend.account.controller;

import com.javaisland.bank_backend.account.dto.AccountHolderDto;
import com.javaisland.bank_backend.account.dto.AccountLimitResponseDto;
import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import com.javaisland.bank_backend.account.dto.CloseAccountRequestDto;
import com.javaisland.bank_backend.account.dto.MonthlySummaryDto;
import com.javaisland.bank_backend.account.dto.OpenAccountRequestDto;
import com.javaisland.bank_backend.account.dto.SetLimitRequestDto;
import com.javaisland.bank_backend.account.service.AccountLimitService;
import com.javaisland.bank_backend.account.service.AccountService;
import com.javaisland.bank_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
public class AccountController {

    private final AccountService accountService;
    private final AccountLimitService accountLimitService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AccountResponseDto>> listMyAccounts(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @PostMapping("/open")
    public ResponseEntity<AccountResponseDto> openAccount(@AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody OpenAccountRequestDto request) {
        Long userId = getUserId(jwt);
        var created = accountService.openAdditionalAccount(userId, request);
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
        Long userId = getUserId(jwt);
        accountService.requestClosure(userId, request.getAccountNumber());
        return ResponseEntity.ok("Closure request submitted. An employee will review it shortly.");
    }

    @GetMapping("/last-active-check")
    public ResponseEntity<Boolean> isLastActiveAccount(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountService.isLastActiveAccount(userId));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDto> getDetail(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable String accountNumber) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountService.getAccountDetail(userId, accountNumber));
    }

    @GetMapping("/{accountNumber}/holder-info")
    public ResponseEntity<AccountHolderDto> getHolderInfo(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountHolderInfo(accountNumber));
    }

    @GetMapping("/{accountNumber}/monthly-summary")
    public ResponseEntity<MonthlySummaryDto> getMonthlySummary(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable String accountNumber) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountService.getMonthlySummary(userId, accountNumber));
    }

    @GetMapping("/{accountNumber}/limits")
    public ResponseEntity<List<AccountLimitResponseDto>> listLimits(@AuthenticationPrincipal Jwt jwt,
                                                                     @PathVariable String accountNumber) {
        Long userId = getUserId(jwt);
        accountService.getAccountDetail(userId, accountNumber);
        return ResponseEntity.ok(accountLimitService.getLimits(accountNumber));
    }

    @PutMapping("/{accountNumber}/limits/{limitType}")
    public ResponseEntity<AccountLimitResponseDto> setLimit(@AuthenticationPrincipal Jwt jwt,
                                                             @PathVariable String accountNumber,
                                                             @PathVariable String limitType,
                                                             @Valid @RequestBody SetLimitRequestDto request) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountLimitService.setLimitAsCustomer(userId, accountNumber, limitType, request));
    }

    private Long getUserId(Jwt jwt) {
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new com.javaisland.bank_backend.exception.ApiBankException(
                        "Utente non trovato.", "USER_NOT_FOUND"))
                .getId();
    }

    @PutMapping("/limits-setup-complete")
    public ResponseEntity<String> completeLimitsSetup(@AuthenticationPrincipal Jwt jwt) {
        var user = userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new com.javaisland.bank_backend.exception.ApiBankException(
                        "Utente non trovato.", "USER_NOT_FOUND"));
        user.setLimitsSetupComplete(true);
        userRepository.save(user);
        return ResponseEntity.ok("Setup completato.");
    }
}
