package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.admin.dto.AdminAccountListItemDto;
import com.javaisland.bank_backend.common.PageResponseDto;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminAccountController {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<PageResponseDto<AdminAccountListItemDto>> listAccounts(
            @RequestParam(required = false) Integer statusId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (statusId != null) {
            List<Account> accounts = accountRepository.findByStatusId(statusId);
            List<AdminAccountListItemDto> fullList = accounts.stream().map(a -> toListItem(a)).toList();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), fullList.size());
            List<AdminAccountListItemDto> paginatedList = fullList.subList(start, end);
            return ResponseEntity.ok(PageResponseDto.from(new PageImpl<>(paginatedList, pageable, fullList.size())));
        }

        Page<Account> accountPage = accountRepository.findAll(pageable);
        List<AdminAccountListItemDto> dtos = accountPage.stream().map(a -> toListItem(a)).toList();
        PageImpl<AdminAccountListItemDto> resultPage = new PageImpl<>(dtos, pageable, accountPage.getTotalElements());
        return ResponseEntity.ok(PageResponseDto.from(resultPage));
    }

    private AdminAccountListItemDto toListItem(Account a) {
        User user = a.getUser();
        var dto = new AdminAccountListItemDto();
        dto.setAccountNumber(a.getAccountNumber());
        dto.setBalance(a.getBalance());
        dto.setStatusId(a.getStatusId());
        dto.setUserId(user.getId());
        dto.setUserFullName(user.getFirstName() + " " + user.getLastName());
        dto.setUserEmail(user.getEmail());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setClosedAt(a.getClosedAt());
        return dto;
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AdminAccountListItemDto> getAccountDetail(
            @PathVariable String accountNumber) {
        Account a = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("Account not found", "ACCOUNT_NOT_FOUND"));
        User user = a.getUser();

        var dto = new AdminAccountListItemDto();
        dto.setAccountNumber(a.getAccountNumber());
        dto.setBalance(a.getBalance());
        dto.setStatusId(a.getStatusId());
        dto.setUserId(user.getId());
        dto.setUserFullName(user.getFirstName() + " " + user.getLastName());
        dto.setUserEmail(user.getEmail());
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setClosedAt(a.getClosedAt());

        return ResponseEntity.ok(dto);
    }
}
