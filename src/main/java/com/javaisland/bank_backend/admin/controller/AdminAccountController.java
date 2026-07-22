package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminAccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Data
    public static class AdminAccountListItemDto {
        private String accountNumber;
        private BigDecimal balance;
        private Integer statusId;
        private Long userId;
        private String userFullName;
        private String userEmail;
        private LocalDateTime createdAt;
        private LocalDateTime closedAt;
    }

    @GetMapping
    public ResponseEntity<List<AdminAccountListItemDto>> listAccounts(
            @RequestParam(required = false) Integer statusId) {
        List<Account> accounts;
        if (statusId != null) {
            accounts = accountRepository.findByStatusId(statusId);
        } else {
            accounts = accountRepository.findAll();
        }

        List<AdminAccountListItemDto> result = accounts.stream().map(a -> {
            User user = a.getUser();
            var dto = new AdminAccountListItemDto();
            dto.setAccountNumber(a.getAccountNumber());
            dto.setBalance(a.getBalance());
            dto.setStatusId(a.getStatusId());
            dto.setUserId(user.getId());
            dto.setUserFullName(user.getFirstName() + " " + user.getLastName());
            dto.setUserEmail(user.getEmail());
            dto.setCreatedAt(a.getCreatedAt());
            dto.setClosedAt(a.getClosedAt());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AdminAccountListItemDto> getAccountDetail(
            @PathVariable String accountNumber) {
        Account a = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        User user = a.getUser();

        var dto = new AdminAccountListItemDto();
        dto.setAccountNumber(a.getAccountNumber());
        dto.setBalance(a.getBalance());
        dto.setStatusId(a.getStatusId());
        dto.setUserId(user.getId());
        dto.setUserFullName(user.getFirstName() + " " + user.getLastName());
        dto.setUserEmail(user.getEmail());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setClosedAt(a.getClosedAt());

        return ResponseEntity.ok(dto);
    }
}
