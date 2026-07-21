package com.javaisland.bank_backend.savedbeneficiary.controller;

import com.javaisland.bank_backend.savedbeneficiary.dto.SavedBeneficiaryRequestDto;
import com.javaisland.bank_backend.savedbeneficiary.dto.SavedBeneficiaryResponseDto;
import com.javaisland.bank_backend.savedbeneficiary.service.SavedBeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/saved-beneficiaries")
@PreAuthorize("hasRole('C')")
@RequiredArgsConstructor
public class SavedBeneficiaryController {

    private final SavedBeneficiaryService savedBeneficiaryService;

    @GetMapping
    public ResponseEntity<List<SavedBeneficiaryResponseDto>> list(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(savedBeneficiaryService.listByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<SavedBeneficiaryResponseDto> save(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SavedBeneficiaryRequestDto request) {
        Long userId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(savedBeneficiaryService.save(userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        Long userId = Long.valueOf(jwt.getSubject());
        savedBeneficiaryService.delete(userId, id);
        return ResponseEntity.ok("Saved beneficiary deleted.");
    }
}
