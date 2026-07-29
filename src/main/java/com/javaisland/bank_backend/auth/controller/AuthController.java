package com.javaisland.bank_backend.auth.controller;

import com.javaisland.bank_backend.auth.dto.LoginRequestDto;
import com.javaisland.bank_backend.auth.dto.LoginResponseDto;
import com.javaisland.bank_backend.auth.dto.RegisterRequestDto;
import com.javaisland.bank_backend.auth.service.KeycloakAdminService;
import com.javaisland.bank_backend.auth.service.RegistrationService;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import com.javaisland.bank_backend.user.dto.UserResponseDto;
import com.javaisland.bank_backend.user.service.UserPinService;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final RegistrationService registrationService;
    private final UserRepository userRepository;
    private final UserPinService userPinService;
    private final KeycloakAdminService keycloakAdminService;
    private final RoleTypeRepository roleTypeRepository;
    private final UserStatusRepository userStatusRepository;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        return ResponseEntity.ok(registrationService.register(requestDto));
    }

    @PostMapping("/keycloak-login")
    public ResponseEntity<?> keycloakLogin(@Valid @RequestBody LoginRequestDto request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        try {
            Map<String, Object> tokenResponse = keycloakAdminService.tokenLogin(
                    request.getUsername(), request.getPassword());

            String keycloakToken = (String) tokenResponse.get("access_token");

            String keycloakId;
            try {
                keycloakId = SignedJWT.parse(keycloakToken).getJWTClaimsSet().getSubject();
            } catch (Exception e) {
                throw new com.javaisland.bank_backend.exception.ApiBankException(
                        "Token Keycloak non valido.", "INVALID_TOKEN");
            }

            if (user == null) {
                user = syncKeycloakUserToDb(keycloakId, request.getUsername());
            }

            String userStatus = user.getStatus().getUserStatus();
            if (!"ACTIVE".equals(userStatus)) {
                String message = switch (userStatus) {
                    case "PENDING" -> "Account in attesa di validazione da parte di un impiegato.";
                    case "ANNULLED" -> "Registrazione annullata. Contatta il supporto.";
                    case "SUSPENDED" -> "Account sospeso. Contatta il supporto.";
                    default -> "Account non disponibile.";
                };
                throw new com.javaisland.bank_backend.exception.ApiBankException(message, "ACCOUNT_" + userStatus);
            }

            if (user.getKeycloakId() == null || !user.getKeycloakId().equals(keycloakId)) {
                user.setKeycloakId(keycloakId);
                userRepository.save(user);
            }

            return ResponseEntity.ok(LoginResponseDto.builder()
                    .token(keycloakToken)
                    .role(user.getRoleType().getRoleName())
                    .userId(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .limitsSetupComplete(user.isLimitsSetupComplete())
                    .pinSetupComplete(userPinService.hasPin(user.getId()))
                    .profilePictureUrl(user.getProfilePictureUrl())
                    .build());

        } catch (com.javaisland.bank_backend.exception.ApiBankException e) {
            throw e;
        } catch (Exception e) {
            log.error("Keycloak login error: {}", e.getMessage());
            throw new com.javaisland.bank_backend.exception.ApiBankException(
                    "Keycloak non raggiungibile.", "KEYCLOAK_ERROR");
        }
    }

    private User syncKeycloakUserToDb(String keycloakId, String username) {
        var kcUsers = keycloakAdminService.getAllUsers();
        Map<String, Object> kcUser = kcUsers.stream()
                .filter(u -> keycloakId.equals(u.get("id")))
                .findFirst()
                .orElseThrow(() -> new com.javaisland.bank_backend.exception.ApiBankException(
                        "Utente non trovato su Keycloak.", "USER_NOT_FOUND_KEYCLOAK"));

        String firstName = (String) kcUser.getOrDefault("firstName", "");
        String lastName = (String) kcUser.getOrDefault("lastName", "");
        String email = (String) kcUser.getOrDefault("email", username);

        var customerRole = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new com.javaisland.bank_backend.exception.ApiBankException("Ruolo C non configurato."));
        var activeStatus = userStatusRepository.findByUserStatus("ACTIVE")
                .orElseThrow(() -> new com.javaisland.bank_backend.exception.ApiBankException("Stato ACTIVE non configurato."));

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRoleType(customerRole);
        user.setStatus(activeStatus);

        User saved = userRepository.save(user);
        log.info("Synced Keycloak user to DB: {} (id={}, keycloakId={})", username, saved.getId(), keycloakId);
        return saved;
    }

}
