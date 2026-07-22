package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.model.RoleType;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminCustomerController {

    private final UserRepository userRepository;
    private final RoleTypeRepository roleTypeRepository;
    private final AccountRepository accountRepository;

    @Data
    public static class AdminCustomerListItemDto {
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;
        private String username;
        private String status;
        private int accountCount;
        private BigDecimal totalBalance;
        private LocalDateTime createdAt;
    }

    @Data
    public static class AdminCustomerDetailDto {
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;
        private String username;
        private LocalDate birthDate;
        private String gender;
        private String fiscalCode;
        private String phone;
        private String residence;
        private String birthPlace;
        private String birthProvince;
        private String profession;
        private String status;
        private LocalDateTime createdAt;
        private List<AccountResponseDto> accounts;
    }

    @GetMapping
    public ResponseEntity<List<AdminCustomerListItemDto>> listCustomers() {
        var customerRole = roleTypeRepository.findByRoleName("C").orElse(null);
        if (customerRole == null) return ResponseEntity.ok(List.of());

        List<AdminCustomerListItemDto> result = userRepository.findByRoleTypeOrderByFirstNameAscLastNameAsc(customerRole)
                .stream().map(user -> {
                    var dto = new AdminCustomerListItemDto();
                    dto.setUserId(user.getId());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    dto.setEmail(user.getEmail());
                    dto.setUsername(user.getUsername());
                    dto.setStatus(user.getStatus().getUserStatus());
                    dto.setCreatedAt(user.getCreatedAt());

                    List<Account> accounts = accountRepository.findByUserId(user.getId());
                    dto.setAccountCount(accounts.size());
                    dto.setTotalBalance(accounts.stream()
                            .map(Account::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));

                    return dto;
                }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminCustomerDetailDto> getCustomerDetail(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var dto = new AdminCustomerDetailDto();
        dto.setUserId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setBirthDate(user.getBirthDate());
        dto.setGender(user.getGender());
        dto.setFiscalCode(user.getFiscalCode());
        dto.setPhone(user.getPhone());
        dto.setResidence(user.getResidence());
        dto.setBirthPlace(user.getBirthPlace());
        dto.setBirthProvince(user.getBirthProvince());
        dto.setProfession(user.getProfession());
        dto.setStatus(user.getStatus().getUserStatus());
        dto.setCreatedAt(user.getCreatedAt());

        List<AccountResponseDto> accounts = accountRepository.findByUserId(user.getId())
                .stream().map(a -> AccountResponseDto.builder()
                        .accountNumber(a.getAccountNumber())
                        .balance(a.getBalance())
                        .statusId(a.getStatusId())
                        .profileId(user.getId())
                        .profileFirstName(user.getFirstName())
                        .profileLastName(user.getLastName())
                        .userStatusId(user.getStatus().getId())
                        .initialAmount(a.getInitialAmount())
                        .createdAt(a.getCreatedAt())
                        .closedAt(a.getClosedAt())
                        .build()).toList();
        dto.setAccounts(accounts);

        return ResponseEntity.ok(dto);
    }
}
