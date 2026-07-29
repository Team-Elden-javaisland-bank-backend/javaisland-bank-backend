package com.javaisland.bank_backend.user.controller;

import com.javaisland.bank_backend.common.SecurityUtil;
import com.javaisland.bank_backend.user.dto.PasswordChangeRequestCreateDto;
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
    private final SecurityUtil securityUtil;

    @PostMapping
    public ResponseEntity<String> requestPasswordChange(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PasswordChangeRequestCreateDto request) {

        var user = securityUtil.getUser(jwt);

        passwordChangeService.requestPasswordChange(user.getId(), request);
        return ResponseEntity.ok("Richiesta di cambio password inviata. In attesa di approvazione.");
    }
}
