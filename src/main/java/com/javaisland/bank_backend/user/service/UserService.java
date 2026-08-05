package com.javaisland.bank_backend.user.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.service.AccountManagementService;
import com.javaisland.bank_backend.account.repository.AccountLimitRepository;
import com.javaisland.bank_backend.account.repository.LimitChangeRequestRepository;
import com.javaisland.bank_backend.auth.service.KeycloakAdminService;
import com.javaisland.bank_backend.audit.service.AuditLogService;
import com.javaisland.bank_backend.beneficiary.repository.BeneficiaryRepository;
import com.javaisland.bank_backend.card.repository.CardRepository;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.repository.NotificationRepository;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import com.javaisland.bank_backend.common.PageResponseDto;
import com.javaisland.bank_backend.user.dto.CustomerListItemDto;
import com.javaisland.bank_backend.user.dto.PendingRegistrationDto;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.model.UserStatus;
import com.javaisland.bank_backend.user.repository.PasswordChangeRequestRepository;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.repository.UserPinRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final RoleTypeRepository roleTypeRepository;
    private final AccountRepository accountRepository;
    private final AccountManagementService accountManagementService;
    private final CardService cardService;
    private final KeycloakAdminService keycloakAdminService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final CardRepository cardRepository;
    private final AccountLimitRepository accountLimitRepository;
    private final LimitChangeRequestRepository limitChangeRequestRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordChangeRequestRepository passwordChangeRequestRepository;
    private final UserPinRepository userPinRepository;
    private final TransactionRepository transactionRepository;

    @Value("${app.export.base-dir:./exports}")
    private String exportBaseDir;

    @Transactional(readOnly = true)
    public List<PendingRegistrationDto> getPendingRegistrations() {
        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        List<User> pendingUsers = userRepository.findByStatus(pendingStatus);

        return pendingUsers.stream().map(u ->
            PendingRegistrationDto.builder()
                    .userId(u.getId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .birthDate(u.getBirthDate())
                    .email(u.getEmail())
                    .profilePictureUrl(u.getProfilePictureUrl())
                    .registeredAt(u.getCreatedAt())
                    .build()
        ).toList();
    }

    @Transactional
    public void validateRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));

        if (user.getStatus() != null && "ACTIVE".equals(user.getStatus().getUserStatus())) {
            throw new ApiBankException("USER_ALREADY_ACTIVE", "USER_ALREADY_ACTIVE");
        }

        if (user.getKeycloakId() != null) {
            try {
                keycloakAdminService.setUserEnabled(user.getKeycloakId(), true);
            } catch (Exception e) {
                throw new ApiBankException("KEYCLOAK_ACTIVATION_FAILED", "KEYCLOAK_ACTIVATION_FAILED");
            }
        }

        var activeStatus = userStatusRepository.findByUserStatus("ACTIVE")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        user.setStatus(activeStatus);
        userRepository.save(user);

        try {
            accountManagementService.activateInitialAccountForUser(userId);
            Account account = accountRepository.findByUserId(userId).stream()
                    .filter(a -> a.getStatusId() == AccountStatus.ACTIVE)
                    .findFirst()
                    .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));
            cardService.issueDebitCard(account.getId(), user.getFirstName() + " " + user.getLastName(), "ACTIVE");
            auditLogService.log("REGISTRATION", userId, "VALIDATE", "system",
                    "Registration approved: " + user.getFirstName() + " " + user.getLastName());
            notificationService.send(userId, "ACCOUNT", "Registration approved! Your account is active.", "NOTIF_REGISTRATION_APPROVED", null);
        } catch (Exception e) {
            if (user.getKeycloakId() != null) {
                keycloakAdminService.setUserEnabled(user.getKeycloakId(), false);
            }
            throw e;
        }
    }

    @Transactional
    public void rejectRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));

        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        if (!user.getStatus().getId().equals(pendingStatus.getId())) {
            throw new ApiBankException("INVALID_STATE", "INVALID_STATE");
        }

        if (user.getKeycloakId() != null) {
            keycloakAdminService.setUserEnabled(user.getKeycloakId(), false);
        }

        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        user.setStatus(annulledStatus);
        userRepository.save(user);
        auditLogService.log("REGISTRATION", userId, "REJECT", "system",
                "Registration rejected: " + user.getFirstName() + " " + user.getLastName());
        notificationService.send(userId, "ACCOUNT", "La tua registrazione è stata rifiutata.", "NOTIF_REGISTRATION_REJECTED", null);
        log.info("Employee rejected registration for user id={}", userId);
    }

    @Transactional(readOnly = true)
    public List<PendingRegistrationDto> getAnnulledRegistrations() {
        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        List<User> annulledUsers = userRepository.findByStatus(annulledStatus);

        return annulledUsers.stream().map(u ->
            PendingRegistrationDto.builder()
                    .userId(u.getId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .birthDate(u.getBirthDate())
                    .email(u.getEmail())
                    .profilePictureUrl(u.getProfilePictureUrl())
                    .registeredAt(u.getCreatedAt())
                    .build()
        ).toList();
    }

    @Transactional
    public void reopenRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));

        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        if (!user.getStatus().getId().equals(annulledStatus.getId())) {
            throw new ApiBankException("INVALID_STATE", "INVALID_STATE");
        }

        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        user.setStatus(pendingStatus);
        userRepository.save(user);
        auditLogService.log("REGISTRATION", userId, "REOPEN", "system",
                "Registration reopened: " + user.getFirstName() + " " + user.getLastName());
        log.info("Employee reopened registration for user id={}", userId);
    }

    @Transactional
    public void deleteUserAndAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));

        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        if (!user.getStatus().getId().equals(annulledStatus.getId())) {
            throw new ApiBankException("INVALID_STATE", "INVALID_STATE");
        }

        List<Account> accounts = accountRepository.findByUserId(userId);
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        for (Account account : accounts) {
            cardRepository.findByAccountId(account.getId()).forEach(cardRepository::delete);
            accountLimitRepository.findByAccountId(account.getId()).forEach(accountLimitRepository::delete);
        }
        transactionRepository.deleteByAccountIds(accountIds);
        accountRepository.deleteAll(accounts);

        limitChangeRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(limitChangeRequestRepository::delete);
        beneficiaryRepository.findByUserId(userId).forEach(beneficiaryRepository::delete);
        notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .forEach(notificationRepository::delete);
        passwordChangeRequestRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .forEach(passwordChangeRequestRepository::delete);
        userPinRepository.findByUserId(userId).ifPresent(userPinRepository::delete);

        if (user.getKeycloakId() != null) {
            keycloakAdminService.deleteUser(user.getKeycloakId());
        }

        userRepository.delete(user);
        auditLogService.log("REGISTRATION", userId, "DELETE", "system",
                "User deleted: " + user.getFirstName() + " " + user.getLastName());
        log.info("Employee deleted user and account for user id={}", userId);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CustomerListItemDto> getAllCustomersSortedByName(Pageable pageable, String status) {
        var customerRole = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new ApiBankException("ROLE_NOT_FOUND", "ROLE_NOT_FOUND"));

        var activeStatus = userStatusRepository.findByUserStatus("ACTIVE").orElse(null);
        UserStatus statusFilter;
        if (status == null || status.isBlank()) {
            statusFilter = activeStatus;
        } else if ("ALL".equalsIgnoreCase(status)) {
            statusFilter = null;
        } else {
            statusFilter = userStatusRepository.findByUserStatus(status).orElse(activeStatus);
        }

        final UserStatus filter = statusFilter;
        List<CustomerListItemDto> fullList = userRepository.findByRoleTypeOrderByFirstNameAscLastNameAsc(customerRole)
                .stream()
                .filter(u -> filter == null || u.getStatus().equals(filter))
                .map(u -> CustomerListItemDto.builder()
                        .userId(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .statusId(u.getStatus().getId())
                        .profilePictureUrl(u.getProfilePictureUrl())
                        .build())
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), fullList.size());
        List<CustomerListItemDto> paginatedList = fullList.subList(start, end);
        return PageResponseDto.from(new PageImpl<>(paginatedList, pageable, fullList.size()));
    }

    public void exportCustomersToFile(String fileName) {
        List<User> users = userRepository.findAll();

        var customerRole = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new ApiBankException("ROLE_NOT_FOUND", "ROLE_NOT_FOUND"));

        List<User> sortedCustomers = users.stream()
                .filter(u -> u.getRoleType().getId().equals(customerRole.getId()))
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .toList();

        if (fileName == null || fileName.isBlank()) {
            throw new ApiBankException("INVALID_EXPORT_PATH", "INVALID_EXPORT_PATH");
        }
        Path fileNamePath = Paths.get(fileName);
        if (fileNamePath.getNameCount() != 1 || fileNamePath.isAbsolute()) {
            throw new ApiBankException("INVALID_EXPORT_PATH", "INVALID_EXPORT_PATH");
        }

        try {
            Path base = Paths.get(exportBaseDir).toAbsolutePath().normalize();
            Files.createDirectories(base);
            Path target = base.resolve(fileNamePath.getFileName()).normalize();
            if (!target.startsWith(base)) {
                throw new ApiBankException("INVALID_EXPORT_PATH", "INVALID_EXPORT_PATH");
            }

            try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                writer.write("=== CUSTOMER LIST (SORTED) ===");
                writer.write(System.lineSeparator());
                for (User customer : sortedCustomers) {
                    writer.write(String.format("Name: %s | Surname: %s | Email: %s | Status: %s%n",
                            customer.getFirstName(),
                            customer.getLastName(),
                            customer.getEmail(),
                            customer.getStatus().getUserStatus()));
                }
            }
        } catch (IOException e) {
            throw new ApiBankException("FILE_WRITE_ERROR", "FILE_WRITE_ERROR");
        }
    }
}