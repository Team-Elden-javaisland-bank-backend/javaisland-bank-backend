package com.javaisland.bank_backend.user.controller;

import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.dto.PasswordChangeRequestCreateDto;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.service.PasswordChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/password-change")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
public class PasswordChangeController {

    private final PasswordChangeService passwordChangeService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<String> requestPasswordChange(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PasswordChangeRequestCreateDto request) {

        User user = userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        passwordChangeService.requestPasswordChange(user.getId(), request);
        return ResponseEntity.ok("Richiesta di cambio password inviata. In attesa di approvazione.");
    }
}
