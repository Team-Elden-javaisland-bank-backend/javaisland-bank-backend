package com.javaisland.bank_backend.beneficiary.controller;

import com.javaisland.bank_backend.beneficiary.dto.BeneficiaryRequestDto;
import com.javaisland.bank_backend.beneficiary.dto.BeneficiaryResponseDto;
import com.javaisland.bank_backend.beneficiary.service.BeneficiaryService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/beneficiaries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponseDto>> list(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(beneficiaryService.listByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponseDto> save(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody BeneficiaryRequestDto request) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(beneficiaryService.save(userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@AuthenticationPrincipal Jwt jwt,
                                          @PathVariable Long id) {
        Long userId = getUserId(jwt);
        beneficiaryService.delete(userId, id);
        return ResponseEntity.ok("Beneficiario rimosso.");
    }

    @GetMapping("/check")
    public ResponseEntity<BeneficiaryResponseDto> check(@AuthenticationPrincipal Jwt jwt,
                                                         @RequestParam String accountNumber) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(beneficiaryService.findByAccountNumber(userId, accountNumber));
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<BeneficiaryResponseDto> rename(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable Long id,
                                                          @RequestBody java.util.Map<String, String> body) {
        Long userId = getUserId(jwt);
        String nickname = body.get("nickname");
        if (nickname == null || nickname.isBlank()) {
            throw new com.javaisland.bank_backend.exception.ApiBankException("Nickname obbligatorio.", "INVALID_NICKNAME");
        }
        return ResponseEntity.ok(beneficiaryService.rename(userId, id, nickname));
    }

    private Long getUserId(Jwt jwt) {
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"))
                .getId();
    }
}
