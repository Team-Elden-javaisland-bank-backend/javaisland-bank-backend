package com.javaisland.bank_backend.auth.service;

import com.javaisland.bank_backend.auth.dto.LoginRequestDto;
import com.javaisland.bank_backend.auth.dto.LoginResponseDto;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import com.javaisland.bank_backend.user.service.UserPinService;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserPinService userPinService;
    private final KeycloakAdminService keycloakAdminService;
    private final RoleTypeRepository roleTypeRepository;
    private final UserStatusRepository userStatusRepository;

    @Transactional
    public LoginResponseDto keycloakLogin(LoginRequestDto request) {
        String username = request.getUsername().toLowerCase();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            String userStatus = user.getStatus().getUserStatus();
            if (!"ACTIVE".equals(userStatus)) {
                String errorCode = switch (userStatus) {
                    case "PENDING" -> "ACCOUNT_PENDING";
                    case "ANNULLED" -> "ACCOUNT_ANNULLED";
                    case "SUSPENDED" -> "ACCOUNT_SUSPENDED";
                    default -> "ACCOUNT_UNAVAILABLE";
                };
                throw new ApiBankException(errorCode, errorCode);
            }
        }

        try {
            Map<String, Object> tokenResponse = keycloakAdminService.tokenLogin(username, request.getPassword());

            String keycloakToken = (String) tokenResponse.get("access_token");

            String keycloakId;
            try {
                keycloakId = SignedJWT.parse(keycloakToken).getJWTClaimsSet().getSubject();
            } catch (Exception e) {
                throw new ApiBankException("INVALID_TOKEN", "INVALID_TOKEN");
            }

            if (user == null) {
                user = syncKeycloakUserToDb(keycloakId, username);
                String userStatus = user.getStatus().getUserStatus();
                if (!"ACTIVE".equals(userStatus)) {
                    String errorCode = switch (userStatus) {
                        case "PENDING" -> "ACCOUNT_PENDING";
                        case "ANNULLED" -> "ACCOUNT_ANNULLED";
                        case "SUSPENDED" -> "ACCOUNT_SUSPENDED";
                        default -> "ACCOUNT_UNAVAILABLE";
                    };
                    throw new ApiBankException(errorCode, errorCode);
                }
            }

            if (user.getKeycloakId() == null || !user.getKeycloakId().equals(keycloakId)) {
                user.setKeycloakId(keycloakId);
                userRepository.save(user);
            }

            return LoginResponseDto.builder()
                    .token(keycloakToken)
                    .role(user.getRoleType().getRoleName())
                    .userId(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .limitsSetupComplete(user.isLimitsSetupComplete())
                    .pinSetupComplete(userPinService.hasPin(user.getId()))
                    .profilePictureUrl(user.getProfilePictureUrl())
                    .build();

        } catch (ApiBankException e) {
            throw e;
        } catch (Exception e) {
            log.error("Keycloak login error: {}", e.getMessage());
            throw new ApiBankException("KEYCLOAK_ERROR", "KEYCLOAK_ERROR");
        }
    }

    public LoginResponseDto getUserInfo(org.springframework.security.oauth2.jwt.Jwt jwt) {
        User user = userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));
        return LoginResponseDto.builder()
                .token(null)
                .role(user.getRoleType().getRoleName())
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .limitsSetupComplete(user.isLimitsSetupComplete())
                .pinSetupComplete(userPinService.hasPin(user.getId()))
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }

    private User syncKeycloakUserToDb(String keycloakId, String username) {
        var kcUsers = keycloakAdminService.getAllUsers();
        Map<String, Object> kcUser = kcUsers.stream()
                .filter(u -> keycloakId.equals(u.get("id")))
                .findFirst()
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND_KEYCLOAK", "USER_NOT_FOUND_KEYCLOAK"));

        String firstName = (String) kcUser.getOrDefault("firstName", "");
        String lastName = (String) kcUser.getOrDefault("lastName", "");
        String email = (String) kcUser.getOrDefault("email", username);

        var customerRole = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new ApiBankException("ROLE_NOT_FOUND", "ROLE_NOT_FOUND"));
        var pendingStatus = userStatusRepository.findByUserStatus("PENDING")
                .orElseThrow(() -> new ApiBankException("STATUS_NOT_CONFIGURED", "STATUS_NOT_CONFIGURED"));

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRoleType(customerRole);
        user.setStatus(pendingStatus);

        User saved = userRepository.save(user);
        log.info("Synced Keycloak user to DB: {} (id={}, keycloakId={})", username, saved.getId(), keycloakId);
        return saved;
    }
}
