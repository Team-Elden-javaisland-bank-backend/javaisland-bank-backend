package com.javaisland.bank_backend.user.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.service.AccountService;
import com.javaisland.bank_backend.auth.service.KeycloakAdminService;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.dto.CustomerListItemDto;
import com.javaisland.bank_backend.user.dto.PendingRegistrationDto;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
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

    public UserService(UserRepository userRepository,
                       UserStatusRepository userStatusRepository,
                       RoleTypeRepository roleTypeRepository,
                       AccountRepository accountRepository,
                       AccountService accountService,
                       CardService cardService,
                       KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.roleTypeRepository = roleTypeRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.cardService = cardService;
        this.keycloakAdminService = keycloakAdminService;
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
            Account account = accountService.createInitialAccountForUser(user);
            account.setStatusId(AccountStatus.ACTIVE);
            accountRepository.save(account);
            cardService.issueDebitCard(account.getId(), user.getFirstName() + " " + user.getLastName(), "ACTIVE");
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
        log.info("Employee rejected registration for user id={}", userId);
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