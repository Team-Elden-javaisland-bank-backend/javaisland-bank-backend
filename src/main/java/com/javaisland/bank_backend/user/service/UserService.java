package com.javaisland.bank_backend.user.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.service.AccountService;
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
import com.javaisland.bank_backend.savedbeneficiary.repository.SavedBeneficiaryRepository;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import com.javaisland.bank_backend.user.dto.CustomerListItemDto;
import com.javaisland.bank_backend.user.dto.PendingRegistrationDto;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.PasswordChangeRequestRepository;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.repository.UserPinRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final RoleTypeRepository roleTypeRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final CardService cardService;
    private final KeycloakAdminService keycloakAdminService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final CardRepository cardRepository;
    private final AccountLimitRepository accountLimitRepository;
    private final LimitChangeRequestRepository limitChangeRequestRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final SavedBeneficiaryRepository savedBeneficiaryRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordChangeRequestRepository passwordChangeRequestRepository;
    private final UserPinRepository userPinRepository;

    public UserService(UserRepository userRepository,
                       UserStatusRepository userStatusRepository,
                       RoleTypeRepository roleTypeRepository,
                       AccountRepository accountRepository,
                       AccountService accountService,
                       CardService cardService,
                       KeycloakAdminService keycloakAdminService,
                       AuditLogService auditLogService,
                       NotificationService notificationService,
                       CardRepository cardRepository,
                       AccountLimitRepository accountLimitRepository,
                       LimitChangeRequestRepository limitChangeRequestRepository,
                       BeneficiaryRepository beneficiaryRepository,
                       SavedBeneficiaryRepository savedBeneficiaryRepository,
                       NotificationRepository notificationRepository,
                       PasswordChangeRequestRepository passwordChangeRequestRepository,
                       UserPinRepository userPinRepository) {
        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.roleTypeRepository = roleTypeRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.cardService = cardService;
        this.keycloakAdminService = keycloakAdminService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.cardRepository = cardRepository;
        this.accountLimitRepository = accountLimitRepository;
        this.limitChangeRequestRepository = limitChangeRequestRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.savedBeneficiaryRepository = savedBeneficiaryRepository;
        this.notificationRepository = notificationRepository;
        this.passwordChangeRequestRepository = passwordChangeRequestRepository;
        this.userPinRepository = userPinRepository;
    }

    @Transactional(readOnly = true)
    public List<PendingRegistrationDto> getPendingRegistrations() {
        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("Stato PENDING non configurato."));
        List<User> pendingUsers = userRepository.findByStatus(pendingStatus);

        return pendingUsers.stream().map(u ->
            PendingRegistrationDto.builder()
                    .userId(u.getId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .birthDate(u.getBirthDate())
                    .email(u.getEmail())
                    .registeredAt(u.getCreatedAt())
                    .build()
        ).toList();
    }

    @Transactional
    public void validateRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato con id: " + userId));

        if (user.getStatus() != null && "ACTIVE".equals(user.getStatus().getUserStatus())) {
            throw new ApiBankException("Utente già attivato. Usa l'endpoint di attivazione conti per i conti secondari.", "USER_ALREADY_ACTIVE");
        }

        String keycloakId = null;
        if (user.getPlainPassword() != null && !user.getPlainPassword().isBlank()) {
            try {
                keycloakId = keycloakAdminService.createUser(
                        user.getUsername(),
                        user.getPlainPassword(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        true);
                user.setKeycloakId(keycloakId);
                user.setPlainPassword(null);
            } catch (Exception e) {
                throw new ApiBankException(
                        "Creazione utente Keycloak fallita. Riprova la validazione.", "KEYCLOAK_CREATION_FAILED");
            }
        }

        var activeStatus = userStatusRepository.findByUserStatus("ACTIVE")
                .orElseThrow(() -> new ApiBankException("Stato ACTIVE non configurato."));
        user.setStatus(activeStatus);
        userRepository.save(user);

        try {
            accountService.activateInitialAccountForUser(userId);
            Account account = accountRepository.findByUserId(userId).stream()
                    .filter(a -> a.getStatusId() == AccountStatus.ACTIVE)
                    .findFirst()
                    .orElseThrow(() -> new ApiBankException("Conto non trovato.", "ACCOUNT_NOT_FOUND"));
            cardService.issueDebitCard(account.getId(), user.getFirstName() + " " + user.getLastName(), "ACTIVE");
            auditLogService.log("REGISTRATION", userId, "VALIDATE", "system",
                    "Registrazione approvata: " + user.getFirstName() + " " + user.getLastName());
            notificationService.send(userId, "ACCOUNT", "Registrazione approvata! Il tuo conto è attivo.", "NOTIF_REGISTRATION_APPROVED", null);
        } catch (Exception e) {
            if (keycloakId != null) {
                keycloakAdminService.deleteUser(keycloakId);
                user.setKeycloakId(null);
            }
            throw e;
        }
    }

    @Transactional
    public void rejectRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato con id: " + userId));

        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("Stato PENDING non configurato."));
        if (!user.getStatus().getId().equals(pendingStatus.getId())) {
            throw new ApiBankException("La registrazione non è in stato PENDING.", "INVALID_STATE");
        }

        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("Stato ANNULLED non configurato."));
        user.setStatus(annulledStatus);
        userRepository.save(user);
        auditLogService.log("REGISTRATION", userId, "REJECT", "system",
                "Registrazione rifiutata: " + user.getFirstName() + " " + user.getLastName());
        notificationService.send(userId, "ACCOUNT", "La tua registrazione è stata rifiutata.", "NOTIF_REGISTRATION_REJECTED", null);
        log.info("Employee rejected registration for user id={}", userId);
    }

    @Transactional(readOnly = true)
    public List<PendingRegistrationDto> getAnnulledRegistrations() {
        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("Stato ANNULLED non configurato."));
        List<User> annulledUsers = userRepository.findByStatus(annulledStatus);

        return annulledUsers.stream().map(u ->
            PendingRegistrationDto.builder()
                    .userId(u.getId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .birthDate(u.getBirthDate())
                    .email(u.getEmail())
                    .registeredAt(u.getCreatedAt())
                    .build()
        ).toList();
    }

    @Transactional
    public void reopenRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato con id: " + userId));

        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("Stato ANNULLED non configurato."));
        if (!user.getStatus().getId().equals(annulledStatus.getId())) {
            throw new ApiBankException("La registrazione non è in stato ANNULLED.", "INVALID_STATE");
        }

        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("Stato PENDING non configurato."));
        user.setStatus(pendingStatus);
        userRepository.save(user);
        auditLogService.log("REGISTRATION", userId, "REOPEN", "system",
                "Registrazione riaperta: " + user.getFirstName() + " " + user.getLastName());
        log.info("Employee reopened registration for user id={}", userId);
    }

    @Transactional
    public void deleteUserAndAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato con id: " + userId));

        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("Stato ANNULLED non configurato."));
        if (!user.getStatus().getId().equals(annulledStatus.getId())) {
            throw new ApiBankException("Solo utenti con registrazione rifiutata possono essere eliminati.", "INVALID_STATE");
        }

        List<Account> accounts = accountRepository.findByUserId(userId);
        for (Account account : accounts) {
            cardRepository.findByAccountId(account.getId()).forEach(cardRepository::delete);
            accountLimitRepository.findByAccountId(account.getId()).forEach(accountLimitRepository::delete);
        }
        accountRepository.deleteAll(accounts);

        limitChangeRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(limitChangeRequestRepository::delete);
        beneficiaryRepository.findByUserId(userId).forEach(beneficiaryRepository::delete);
        savedBeneficiaryRepository.findByUserId(userId).forEach(savedBeneficiaryRepository::delete);
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(notificationRepository::delete);
        passwordChangeRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .forEach(passwordChangeRequestRepository::delete);
        userPinRepository.findByUserId(userId).ifPresent(userPinRepository::delete);

        if (user.getKeycloakId() != null) {
            keycloakAdminService.deleteUser(user.getKeycloakId());
        }

        userRepository.delete(user);
        auditLogService.log("REGISTRATION", userId, "DELETE", "system",
                "Utente eliminato: " + user.getFirstName() + " " + user.getLastName());
        log.info("Employee deleted user and account for user id={}", userId);
    }

    @Transactional(readOnly = true)
    public List<CustomerListItemDto> getAllCustomersSortedByName() {
        var customerRole = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new ApiBankException("Ruolo C non configurato."));
        return userRepository.findByRoleTypeOrderByFirstNameAscLastNameAsc(customerRole)
                .stream().map(u -> CustomerListItemDto.builder()
                        .userId(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .statusId(u.getStatus().getId())
                        .build())
                .toList();
    }

    public void exportCustomersToFile(String filePath) {
        List<User> users = userRepository.findAll();

        var customerRole = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new ApiBankException("Ruolo C non configurato."));

        List<User> sortedCustomers = users.stream()
                .filter(u -> u.getRoleType().getId().equals(customerRole.getId()))
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .toList();

        try (FileWriter fileWriter = new FileWriter(filePath);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            printWriter.println("=== ELENCO CORRENTISTI ORDINATO ===");
            for (User customer : sortedCustomers) {
                printWriter.printf("Nome: %s | Cognome: %s | Email: %s | Stato: %s%n",
                        customer.getFirstName(),
                        customer.getLastName(),
                        customer.getEmail(),
                        customer.getStatus().getUserStatus());
            }

        } catch (IOException e) {
            throw new ApiBankException("Errore durante la scrittura del file dei correntisti.");
        }
    }
}