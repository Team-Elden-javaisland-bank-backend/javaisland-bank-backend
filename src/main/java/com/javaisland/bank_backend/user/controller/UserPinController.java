package com.javaisland.bank_backend.user.controller;

import com.javaisland.bank_backend.common.SecurityUtil;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.dto.PinSetupRequestDto;
import com.javaisland.bank_backend.user.dto.PinStatusResponseDto;
import com.javaisland.bank_backend.user.dto.PinVerifyRequestDto;
import com.javaisland.bank_backend.user.service.UserPinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/pin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
public class UserPinController {

    private final UserPinService userPinService;
    private final SecurityUtil securityUtil;

    @PostMapping("/setup")
    public ResponseEntity<PinStatusResponseDto> setupPin(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PinSetupRequestDto request) {

        Long userId = securityUtil.getUserId(jwt);
        userPinService.setupPin(userId, request.getPin());

        PinStatusResponseDto response = new PinStatusResponseDto();
        response.setPinSetupComplete(true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<PinStatusResponseDto> getPinStatus(
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = securityUtil.getUserId(jwt);

        PinStatusResponseDto response = new PinStatusResponseDto();
        response.setPinSetupComplete(userPinService.hasPin(userId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<PinStatusResponseDto> verifyPin(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PinVerifyRequestDto request) {

        Long userId = securityUtil.getUserId(jwt);
        boolean valid = userPinService.verifyPin(userId, request.getPin());
        if (!valid) {
            throw new ApiBankException("INVALID_PIN", "INVALID_PIN");
        }

        PinStatusResponseDto response = new PinStatusResponseDto();
        response.setPinSetupComplete(true);
        return ResponseEntity.ok(response);
    }
}
