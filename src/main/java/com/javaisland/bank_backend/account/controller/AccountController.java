package com.javaisland.bank_backend.account.controller;

import com.javaisland.bank_backend.account.dto.AccountActionResponseDto;
import com.javaisland.bank_backend.account.dto.AccountHolderDto;
import com.javaisland.bank_backend.account.dto.AccountLimitResponseDto;
import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import com.javaisland.bank_backend.account.dto.CloseAccountRequestDto;
import com.javaisland.bank_backend.account.dto.DashboardSummaryDto;
import com.javaisland.bank_backend.account.dto.MonthlySummaryDto;
import com.javaisland.bank_backend.account.dto.OpenAccountRequestDto;
import com.javaisland.bank_backend.account.dto.SetLimitRequestDto;
import com.javaisland.bank_backend.account.service.AccountLimitService;
import com.javaisland.bank_backend.account.service.AccountManagementService;
import com.javaisland.bank_backend.account.service.AccountQueryService;
import com.javaisland.bank_backend.common.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Customer Accounts", description = "Account management for customers")
public class AccountController {

    private final AccountManagementService accountManagementService;
    private final AccountQueryService accountQueryService;
    private final AccountLimitService accountLimitService;
    private final SecurityUtil securityUtil;

    @GetMapping
    @Operation(summary = "List my accounts", description = "Returns all accounts for the authenticated customer")
    public ResponseEntity<List<AccountResponseDto>> listMyAccounts(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountQueryService.getAccountsByUserId(userId));
    }

    @GetMapping("/dashboard-summary")
    @Operation(summary = "Dashboard summary", description = "Returns aggregated balance summary for the customer")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountQueryService.getDashboardSummary(userId));
    }

    @PostMapping("/open")
    @Operation(summary = "Open additional account", description = "Creates a new account request with initial transfer from existing account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account opening request submitted"),
        @ApiResponse(responseCode = "400", description = "Validation error or business rule violation")
    })
    public ResponseEntity<AccountResponseDto> openAccount(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody OpenAccountRequestDto request) {
        Long userId = getUserId(jwt);
        var created = accountManagementService.openAdditionalAccount(userId, request);
        return ResponseEntity.ok(AccountResponseDto.builder()
                .accountNumber(created.getAccountNumber())
                .balance(created.getBalance())
                .statusId(created.getStatusId())
                .isLimitsConfigured(created.getIsLimitsConfigured())
                .createdAt(created.getCreatedAt())
                .build());
    }

    @PostMapping("/closure-request")
    @Operation(summary = "Request account closure", description = "Submits a closure request for the specified account")
    public ResponseEntity<AccountActionResponseDto> requestClosure(@AuthenticationPrincipal Jwt jwt,
                                                                   @Valid @RequestBody CloseAccountRequestDto request) {
        Long userId = getUserId(jwt);
        accountManagementService.requestClosure(userId, request.getAccountNumber());
        return ResponseEntity.ok(AccountActionResponseDto.builder()
                .messageKey("ACCOUNTS.CLOSE_REQUEST_SUCCESS")
                .status("PENDING")
                .build());
    }

    @GetMapping("/last-active-check")
    @Operation(summary = "Check last active account", description = "Returns whether this is the customer's last active account")
    public ResponseEntity<Boolean> isLastActiveAccount(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountQueryService.isLastActiveAccount(userId));
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account detail", description = "Returns detailed information for the specified account")
    public ResponseEntity<AccountResponseDto> getDetail(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable String accountNumber) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountQueryService.getAccountDetail(userId, accountNumber));
    }

    @GetMapping("/{accountNumber}/holder-info")
    @Operation(summary = "Get account holder info", description = "Returns basic holder information for the specified account")
    public ResponseEntity<AccountHolderDto> getHolderInfo(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountQueryService.getAccountHolderInfo(accountNumber));
    }

    @GetMapping("/{accountNumber}/monthly-summary")
    @Operation(summary = "Get monthly summary", description = "Returns income and movement count for the current month")
    public ResponseEntity<MonthlySummaryDto> getMonthlySummary(@AuthenticationPrincipal Jwt jwt,
                                                                 @PathVariable String accountNumber) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountQueryService.getMonthlySummary(userId, accountNumber));
    }

    @GetMapping("/{accountNumber}/limits")
    @Operation(summary = "List account limits", description = "Returns all configured limits for the specified account")
    public ResponseEntity<List<AccountLimitResponseDto>> listLimits(@AuthenticationPrincipal Jwt jwt,
                                                                      @PathVariable String accountNumber) {
        Long userId = getUserId(jwt);
        accountQueryService.getAccountDetail(userId, accountNumber);
        return ResponseEntity.ok(accountLimitService.getLimits(accountNumber));
    }

    @PutMapping("/{accountNumber}/limits/{limitType}")
    @Operation(summary = "Set account limit", description = "Sets or updates a limit for the specified account")
    public ResponseEntity<AccountLimitResponseDto> setLimit(@AuthenticationPrincipal Jwt jwt,
                                                              @PathVariable String accountNumber,
                                                              @PathVariable String limitType,
                                                              @Valid @RequestBody SetLimitRequestDto request) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(accountLimitService.setLimitAsCustomer(userId, accountNumber, limitType, request));
    }

    @PutMapping("/limits-setup-complete")
    @Operation(summary = "Complete limits setup", description = "Marks the initial limits setup as complete for the customer")
    public ResponseEntity<Void> completeLimitsSetup(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        accountManagementService.completeLimitsSetup(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{accountNumber}/limits-config-complete")
    @Operation(summary = "Complete account limits config", description = "Marks the specified active account as having its limits configured")
    public ResponseEntity<Void> completeAccountLimitsConfig(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable String accountNumber) {
        Long userId = getUserId(jwt);
        accountManagementService.markLimitsConfigured(userId, accountNumber);
        return ResponseEntity.ok().build();
    }

    private Long getUserId(Jwt jwt) {
        return securityUtil.getUserId(jwt);
    }
}
