package com.javaisland.bank_backend.account.controller;

import com.javaisland.bank_backend.account.dto.LimitChangeRequestCreateDto;
import com.javaisland.bank_backend.account.service.LimitChangeService;
import com.javaisland.bank_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer/limit-change")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
public class LimitChangeController {

    private final LimitChangeService limitChangeService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<String> requestLimitChange(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LimitChangeRequestCreateDto request) {
        Long userId = userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new com.javaisland.bank_backend.exception.ApiBankException(
                        "Utente non trovato.", "USER_NOT_FOUND"))
                .getId();
        limitChangeService.requestLimitChange(userId, request);
        return ResponseEntity.ok("Richiesta di modifica limite inviata. In attesa di approvazione.");
    }
}
