package com.javaisland.bank_backend.auth.service;

import com.javaisland.bank_backend.account.service.AccountService;
import com.javaisland.bank_backend.auth.dto.RegisterRequestDto;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.dto.UserResponseDto;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final RoleTypeRepository roleTypeRepository;
    private final UserStatusRepository userStatusRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Transactional(propagation = Propagation.REQUIRED)
    public User createPendingUser(RegisterRequestDto request) {
        String email = request.getEmail().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ApiBankException("Email già registrata.", "EMAIL_ALREADY_REGISTERED");
        }
        if (userRepository.findByUsername(email).isPresent()) {
            throw new ApiBankException("Username già registrato.", "USERNAME_ALREADY_REGISTERED");
        }
        var customerRole = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new ApiBankException("ROLE_NOT_FOUND", "ROLE_NOT_FOUND"));
        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));

        String keycloakId;
        try {
            keycloakId = keycloakAdminService.createUser(
                    email,
                    request.getPassword(),
                    email,
                    request.getFirstName(),
                    request.getLastName(),
                    false);
        } catch (ApiBankException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiBankException("Creazione utente Keycloak fallita.", "KEYCLOAK_CREATION_FAILED");
        }

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBirthDate(request.getBirthDate());
        user.setEmail(email);
        user.setUsername(email);
        user.setRoleType(customerRole);
        user.setStatus(pendingStatus);
        user.setProfession(request.getProfession());
        user.setGender(request.getGender());
        user.setFiscalCode(request.getFiscalCode());
        user.setPhone(request.getPhone());
        user.setResidence(request.getResidence());
        user.setBirthPlace(request.getBirthPlace());
        user.setBirthProvince(request.getBirthProvince());
        return userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void annulUser(Long userId) {
        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        userRepository.findById(userId).ifPresent(u -> {
            if (u.getKeycloakId() != null) {
                keycloakAdminService.deleteUser(u.getKeycloakId());
            }
            u.setStatus(annulledStatus);
            userRepository.save(u);
            log.warn("User id={} ANNULLED because the associated account could not be created", userId);
        });
    }

    @Transactional
    public UserResponseDto register(RegisterRequestDto request) {
        User user = createPendingUser(request);

        try {
            accountService.createInitialAccountForUser(user);
        } catch (Exception e) {
            log.error("Account creation failed during registration for user id={}: {}", user.getId(), e.getMessage(), e);
            throw new ApiBankException(
                    "Registrazione fallita: impossibile creare il conto. Riprova.",
                    "ACCOUNT_CREATION_FAILED");
        }

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .statusId(user.getStatus().getId())
                .profession(user.getProfession())
                .gender(user.getGender())
                .fiscalCode(user.getFiscalCode())
                .phone(user.getPhone())
                .residence(user.getResidence())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        if (!user.getStatus().getId().equals(pendingStatus.getId())) {
            throw new ApiBankException("La registrazione non è in stato PENDING.", "INVALID_STATE");
        }

        if (user.getKeycloakId() != null) {
            keycloakAdminService.deleteUser(user.getKeycloakId());
        }

        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_FOUND", "STATUS_NOT_FOUND"));
        user.setStatus(annulledStatus);
        userRepository.save(user);
        log.info("Registration cancelled for user id={}", userId);
    }
}
