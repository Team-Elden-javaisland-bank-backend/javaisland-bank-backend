package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.account.dto.AccountLimitResponseDto;
import com.javaisland.bank_backend.account.dto.SetLimitRequestDto;
import com.javaisland.bank_backend.account.service.AccountLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminLimitController {

    private final AccountLimitService accountLimitService;

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
