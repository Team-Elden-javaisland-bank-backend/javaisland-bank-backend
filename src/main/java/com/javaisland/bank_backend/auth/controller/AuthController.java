package com.javaisland.bank_backend.auth.controller;

import com.javaisland.bank_backend.auth.dto.LoginRequestDto;
import com.javaisland.bank_backend.auth.dto.LoginResponseDto;
import com.javaisland.bank_backend.auth.dto.RegisterRequestDto;
import com.javaisland.bank_backend.auth.service.RegistrationService;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.dto.UserResponseDto;
import com.javaisland.bank_backend.user.service.UserPinService;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final RegistrationService registrationService;
    private final UserRepository userRepository;
    private final UserPinService userPinService;

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        return ResponseEntity.ok(registrationService.register(requestDto));
    }

    @PostMapping("/keycloak-login")
    public ResponseEntity<?> keycloakLogin(@Valid @RequestBody LoginRequestDto request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new com.javaisland.bank_backend.exception.ApiBankException(
                        "Utente non trovato.", "USER_NOT_FOUND"));

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

        try {
            RestTemplate restTemplate = new RestTemplate();
            String tokenUrl = keycloakAuthUrl + "/realms/" + keycloakRealm
                    + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", keycloakClientId);
            body.add("username", request.getUsername());
            body.add("password", request.getPassword());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new com.javaisland.bank_backend.exception.ApiBankException(
                        "Autenticazione Keycloak fallita.", "INVALID_CREDENTIALS");
            }

            @SuppressWarnings("unchecked")
            String keycloakToken = (String) response.getBody().get("access_token");

            String keycloakId;
            try {
                keycloakId = SignedJWT.parse(keycloakToken).getJWTClaimsSet().getSubject();
            } catch (Exception e) {
                throw new com.javaisland.bank_backend.exception.ApiBankException(
                        "Token Keycloak non valido.", "INVALID_TOKEN");
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
                    .build());

        } catch (com.javaisland.bank_backend.exception.ApiBankException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            throw new com.javaisland.bank_backend.exception.ApiBankException(
                    "Credenziali non valide.", "INVALID_CREDENTIALS");
        } catch (Exception e) {
            log.error("Keycloak login error: {}", e.getMessage());
            throw new com.javaisland.bank_backend.exception.ApiBankException(
                    "Keycloak non raggiungibile.", "KEYCLOAK_ERROR");
        }
    }

}
