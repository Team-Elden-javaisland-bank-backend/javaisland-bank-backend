package com.javaisland.bank_backend.card.controller;

import com.javaisland.bank_backend.card.service.CardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PatchMapping("/{cardId}/status")
    @PreAuthorize("hasRole('D')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long cardId,
            @RequestParam String status) {

        cardService.updateCardStatus(cardId, status);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}