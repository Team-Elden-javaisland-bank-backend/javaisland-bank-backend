package com.javaisland.bank_backend.authentication;

import com.javaisland.bank_backend.account.Account;
import com.javaisland.bank_backend.account.AccountService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.RoleType;
import com.javaisland.bank_backend.user.User;
import com.javaisland.bank_backend.user.UserRepository;
import com.javaisland.bank_backend.user.UserResponseDto;
import com.javaisland.bank_backend.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserRepository userRepository;
    private final AccountService accountService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createPendingUser(RegisterRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiBankException("Email already registered.", "EMAIL_ALREADY_REGISTERED");
        }
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBirthDate(request.getBirthDate());
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail());
        user.setKeycloakId(UUID.randomUUID().toString()); // TODO: replace with real Keycloak user provisioning
        user.setRoleTypeId(RoleType.CUSTOMER);
        user.setStatusId(UserStatus.PENDING);
        return userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void annulUser(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setStatusId(UserStatus.ANNULLED);
            userRepository.save(u);
            log.warn("User id={} ANNULLED because the associated account could not be created", userId);
        });
    }

    public UserResponseDto register(RegisterRequestDto request) {
        User user = createPendingUser(request);

        Account account;
        try {
            account = accountService.createInitialAccountForUser(user);
        } catch (Exception e) {
            annulUser(user.getId());
            // distinguishable error code so the FE can show a specific message
            throw new ApiBankException(
                    "Registration could not be completed: the bank account could not be created. Your request has been annulled, please try registering again.",
                    "REGISTRATION_ANNULLED");
        }

        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .statusId(user.getStatusId())
                .build();
    }
}
