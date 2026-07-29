package com.javaisland.bank_backend.card.controller;

import com.javaisland.bank_backend.card.dto.CardResponseDto;
import com.javaisland.bank_backend.card.dto.CardSensitiveDto;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.common.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('C')")
public class CustomerCardController {

    private final CardService cardService;
    private final SecurityUtil securityUtil;

    @GetMapping
    public ResponseEntity<List<CardResponseDto>> listMyCards(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(cardService.getCardsByUserId(userId));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponseDto> getMyCard(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable Long cardId) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(cardService.getCardDetailForUser(userId, cardId));
    }

    @GetMapping("/{cardId}/sensitive")
    public ResponseEntity<CardSensitiveDto> getMyCardSensitive(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable Long cardId) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(cardService.getCardSensitiveForUser(userId, cardId));
    }

    private Long getUserId(Jwt jwt) {
        return securityUtil.getUserId(jwt);
    }
}
