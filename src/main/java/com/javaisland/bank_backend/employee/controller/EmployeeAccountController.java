package com.javaisland.bank_backend.employee.controller;

import com.javaisland.bank_backend.account.dto.AccountLimitResponseDto;
import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import com.javaisland.bank_backend.account.dto.EmployeeUserDetailDto;
import com.javaisland.bank_backend.account.dto.SetLimitRequestDto;
import com.javaisland.bank_backend.account.service.AccountLimitService;
import com.javaisland.bank_backend.account.service.AccountManagementService;
import com.javaisland.bank_backend.account.service.AccountQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('D')")
public class EmployeeAccountController {

    private final AccountManagementService accountManagementService;
    private final AccountQueryService accountQueryService;
    private final AccountLimitService accountLimitService;

    @GetMapping
    public ResponseEntity<List<AccountResponseDto>> listAccounts(
            @RequestParam(required = false) Integer status) {
        List<AccountResponseDto> accounts = accountQueryService.getAllAccountsByStatus(status);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponseDto>> getAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountQueryService.getAccountsByUserId(userId));
    }

    @PutMapping("/{accountNumber}/activate")
    public ResponseEntity<Void> activate(@PathVariable String accountNumber) {
        accountManagementService.activateAccount(accountNumber);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{accountNumber}/reject")
    public ResponseEntity<Void> reject(@PathVariable String accountNumber) {
        accountManagementService.rejectAccountRequest(accountNumber);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{accountNumber}/closure/validate")
    public ResponseEntity<Void> validateClosure(@PathVariable String accountNumber) {
        accountManagementService.validateClosure(accountNumber);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{accountNumber}/closure/reject")
    public ResponseEntity<Void> rejectClosure(@PathVariable String accountNumber) {
        accountManagementService.rejectClosure(accountNumber);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{accountNumber}/freeze")
    public ResponseEntity<Void> freeze(@PathVariable String accountNumber) {
        accountManagementService.freezeAccount(accountNumber);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{accountNumber}/unfreeze")
    public ResponseEntity<Void> unfreeze(@PathVariable String accountNumber) {
        accountManagementService.unfreezeAccount(accountNumber);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDto> getDetail(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountQueryService.getAccountDetailAsEmployee(accountNumber));
    }

    @GetMapping("/{accountNumber}/user-detail")
    public ResponseEntity<EmployeeUserDetailDto> getUserDetail(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountQueryService.getEmployeeUserDetail(accountNumber));
    }

    @GetMapping("/{accountNumber}/limits")
    public ResponseEntity<List<AccountLimitResponseDto>> listLimits(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountLimitService.getLimits(accountNumber));
    }

    @PutMapping("/{accountNumber}/limits/{limitType}")
    public ResponseEntity<AccountLimitResponseDto> setLimit(@PathVariable String accountNumber,
                                                             @PathVariable String limitType,
                                                             @Valid @RequestBody SetLimitRequestDto request) {
        return ResponseEntity.ok(accountLimitService.setLimit(accountNumber, limitType, request));
    }
}
