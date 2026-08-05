package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.admin.dto.AdminCustomerDetailDto;
import com.javaisland.bank_backend.admin.dto.AdminCustomerListItemDto;
import com.javaisland.bank_backend.auth.service.KeycloakAdminService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.model.RoleType;
import com.javaisland.bank_backend.user.model.UserStatus;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import com.javaisland.bank_backend.common.PageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
@Slf4j
public class AdminCustomerController {

    private final UserRepository userRepository;
    private final RoleTypeRepository roleTypeRepository;
    private final AccountRepository accountRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final UserStatusRepository userStatusRepository;

    @GetMapping
    public ResponseEntity<PageResponseDto<AdminCustomerListItemDto>> listCustomers(
            @PageableDefault(size = 20, sort = "firstName") Pageable pageable) {
        var customerRole = roleTypeRepository.findByRoleName("C").orElse(null);
        if (customerRole == null) return ResponseEntity.ok(PageResponseDto.from(new PageImpl<AdminCustomerListItemDto>(List.of())));

        List<User> users = userRepository.findByRoleTypeOrderByFirstNameAscLastNameAsc(customerRole);
        List<Long> userIds = users.stream().map(User::getId).toList();
        List<Account> allAccounts = accountRepository.findByUserIdIn(userIds);
        var accountsByUser = allAccounts.stream().collect(java.util.stream.Collectors.groupingBy(a -> a.getUser().getId()));

        List<AdminCustomerListItemDto> fullList = users.stream().map(user -> {
            List<Account> userAccounts = accountsByUser.getOrDefault(user.getId(), List.of());
            var dto = new AdminCustomerListItemDto();
            dto.setUserId(user.getId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setUsername(user.getUsername());
            dto.setStatus(user.getStatus().getUserStatus());
            dto.setProfilePictureUrl(user.getProfilePictureUrl());
            dto.setCreatedAt(user.getCreatedAt());
            dto.setAccountCount(userAccounts.size());
            dto.setTotalBalance(userAccounts.stream()
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            return dto;
        }).toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), fullList.size());
        List<AdminCustomerListItemDto> paginatedList = fullList.subList(start, end);
        return ResponseEntity.ok(PageResponseDto.from(new PageImpl<>(paginatedList, pageable, fullList.size())));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AdminCustomerDetailDto> getCustomerDetail(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("User not found", "USER_NOT_FOUND"));

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
        dto.setProfilePictureUrl(user.getProfilePictureUrl());
        dto.setCreatedAt(user.getCreatedAt());

        List<AccountResponseDto> accounts = accountRepository.findByUserId(user.getId())
                .stream().map(a -> AccountResponseDto.builder()
                        .accountNumber(a.getAccountNumber())
                        .balance(a.getBalance())
                        .statusId(a.getStatusId())
                        .profileId(user.getId())
                        .profileFirstName(user.getFirstName())
                        .profileLastName(user.getLastName())
                        .profilePictureUrl(user.getProfilePictureUrl())
                        .userStatusId(user.getStatus().getId())
                        .initialAmount(a.getInitialAmount())
                        .createdAt(a.getCreatedAt())
                        .closedAt(a.getClosedAt())
                        .build()).toList();
        dto.setAccounts(accounts);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/sync-keycloak")
    public ResponseEntity<Map<String, Object>> syncKeycloakUsers() {
        var kcUsers = keycloakAdminService.getAllUsers();
        var customerRole = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new ApiBankException("Role C not configured", "ROLE_NOT_FOUND"));
        var activeStatus = userStatusRepository.findByUserStatus("ACTIVE")
                .orElseThrow(() -> new ApiBankException("Status ACTIVE not configured", "STATUS_NOT_FOUND"));

        int synced = 0;
        for (Map<String, Object> kcUser : kcUsers) {
            String keycloakId = (String) kcUser.get("id");
            String username = (String) kcUser.get("username");

            if (userRepository.findByKeycloakId(keycloakId).isPresent()) {
                continue;
            }
            if (userRepository.findByUsername(username).isPresent()) {
                User existing = userRepository.findByUsername(username).get();
                existing.setKeycloakId(keycloakId);
                userRepository.save(existing);
                synced++;
                log.info("Linked existing DB user '{}' to Keycloak id={}", username, keycloakId);
                continue;
            }

            String firstName = (String) kcUser.getOrDefault("firstName", "");
            String lastName = (String) kcUser.getOrDefault("lastName", "");
            String email = (String) kcUser.getOrDefault("email", username);

            User user = new User();
            user.setKeycloakId(keycloakId);
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setRoleType(customerRole);
            user.setStatus(activeStatus);
            userRepository.save(user);
            synced++;
            log.info("Synced Keycloak user to DB: {} (keycloakId={})", username, keycloakId);
        }

        return ResponseEntity.ok(Map.of(
                "keycloakUsers", kcUsers.size(),
                "synced", synced
        ));
    }
}
