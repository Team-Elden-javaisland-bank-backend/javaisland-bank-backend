package com.javaisland.bank_backend.user.controller;

import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.dto.PinSetupRequestDto;
import com.javaisland.bank_backend.user.dto.PinStatusResponseDto;
import com.javaisland.bank_backend.user.dto.PinVerifyRequestDto;
import com.javaisland.bank_backend.user.service.UserPinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pin")
@RequiredArgsConstructor
public class UserPinController {

    private final UserPinService userPinService;

    @PostMapping("/setup")
    public ResponseEntity<PinStatusResponseDto> setupPin(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PinSetupRequestDto request) {

        userPinService.setupPin(userId, request.getPin());

        PinStatusResponseDto response = new PinStatusResponseDto();
        response.setPinSetupComplete(true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<PinStatusResponseDto> getPinStatus(
            @RequestHeader("X-User-Id") Long userId) {

        PinStatusResponseDto response = new PinStatusResponseDto();
        response.setPinSetupComplete(userPinService.hasPin(userId));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<PinStatusResponseDto> verifyPin(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody PinVerifyRequestDto request) {

        boolean valid = userPinService.verifyPin(userId, request.getPin());
        if (!valid) {
            throw new ApiBankException("PIN errato.", "INVALID_PIN");
        }

        PinStatusResponseDto response = new PinStatusResponseDto();
        response.setPinSetupComplete(true);
        return ResponseEntity.ok(response);
    }
}
