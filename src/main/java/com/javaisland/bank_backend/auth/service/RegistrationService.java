package com.javaisland.bank_backend.auth.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.service.AccountService;
import com.javaisland.bank_backend.auth.dto.RegisterRequestDto;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.RoleType;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.dto.UserResponseDto;
import com.javaisland.bank_backend.user.model.UserStatus;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final RoleTypeRepository roleTypeRepository;
    private final UserStatusRepository userStatusRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public User createPendingUser(RegisterRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiBankException("Email già registrata.", "EMAIL_ALREADY_REGISTERED");
        }
        if (userRepository.findByUsername(request.getEmail()).isPresent()) {
            throw new ApiBankException("Username già registrato.", "USERNAME_ALREADY_REGISTERED");
        }
        var customerRole = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new ApiBankException("Ruolo C non configurato."));
        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("Stato PENDING non configurato."));
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBirthDate(request.getBirthDate());
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail());
        user.setPassword(hashPassword(request.getPassword()));
        user.setKeycloakId(null);
        user.setPlainPassword(request.getPassword());
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
                .orElseThrow(() -> new ApiBankException("Stato ANNULLED non configurato."));
        userRepository.findById(userId).ifPresent(u -> {
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
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("Stato PENDING non configurato."));
        if (!user.getStatus().getId().equals(pendingStatus.getId())) {
            throw new ApiBankException("La registrazione non è in stato PENDING.", "INVALID_STATE");
        }

        var annulledStatus = userStatusRepository.findByUserStatus("ANNULLED")
                .orElseThrow(() -> new ApiBankException("Stato ANNULLED non configurato."));
        user.setStatus(annulledStatus);
        userRepository.save(user);
        log.info("Registration cancelled for user id={}", userId);
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
