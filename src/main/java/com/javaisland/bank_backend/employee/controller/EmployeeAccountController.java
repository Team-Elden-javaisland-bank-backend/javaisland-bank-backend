package com.javaisland.bank_backend.employee.controller;

import com.javaisland.bank_backend.account.dto.AccountLimitResponseDto;
import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import com.javaisland.bank_backend.account.dto.EmployeeUserDetailDto;
import com.javaisland.bank_backend.account.dto.SetLimitRequestDto;
import com.javaisland.bank_backend.account.service.AccountLimitService;
import com.javaisland.bank_backend.account.service.AccountService;
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

    private final AccountService accountService;
    private final AccountLimitService accountLimitService;

    @GetMapping
    public ResponseEntity<List<AccountResponseDto>> listAccounts(
            @RequestParam(required = false) Integer status) {
        List<AccountResponseDto> accounts = accountService.getAllAccountsByStatus(status);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponseDto>> getAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getAccountsByUserId(userId));
    }

    @PutMapping("/{accountNumber}/activate")
    public ResponseEntity<String> activate(@PathVariable String accountNumber) {
        accountService.activateAccount(accountNumber);
        return ResponseEntity.ok("Conto " + accountNumber + " attivato.");
    }

    @PutMapping("/{accountNumber}/reject")
    public ResponseEntity<String> reject(@PathVariable String accountNumber) {
        accountService.rejectAccountRequest(accountNumber);
        return ResponseEntity.ok("Richiesta conto " + accountNumber + " rifiutata.");
    }

    @PutMapping("/{accountNumber}/closure/validate")
    public ResponseEntity<String> validateClosure(@PathVariable String accountNumber) {
        accountService.validateClosure(accountNumber);
        return ResponseEntity.ok("Conto " + accountNumber + " chiuso.");
    }

    @PutMapping("/{accountNumber}/closure/reject")
    public ResponseEntity<String> rejectClosure(@PathVariable String accountNumber) {
        accountService.rejectClosure(accountNumber);
        return ResponseEntity.ok("Richiesta di chiusura rifiutata, conto " + accountNumber + " di nuovo attivo.");
    }

    @PutMapping("/{accountNumber}/freeze")
    public ResponseEntity<String> freeze(@PathVariable String accountNumber) {
        accountService.freezeAccount(accountNumber);
        return ResponseEntity.ok("Conto " + accountNumber + " congelato.");
    }

    @PutMapping("/{accountNumber}/unfreeze")
    public ResponseEntity<String> unfreeze(@PathVariable String accountNumber) {
        accountService.unfreezeAccount(accountNumber);
        return ResponseEntity.ok("Conto " + accountNumber + " scongelato.");
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDto> getDetail(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountDetailAsEmployee(accountNumber));
    }

    @GetMapping("/{accountNumber}/user-detail")
    public ResponseEntity<EmployeeUserDetailDto> getUserDetail(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getEmployeeUserDetail(accountNumber));
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
