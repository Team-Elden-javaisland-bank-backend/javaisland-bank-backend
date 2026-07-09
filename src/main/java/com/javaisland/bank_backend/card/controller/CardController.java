package com.javaisland.bank_backend.card.controller;

import com.javaisland.bank_backend.card.model.Card;
import com.javaisland.bank_backend.card.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PatchMapping("/{cardId}/status")
    @PreAuthorize("hasRole('D')")
    public ResponseEntity<Card> updateStatus(
            @PathVariable Long cardId,
            @RequestParam String status) {

        Card updatedCard = cardService.updateCardStatus(cardId, status);
        return ResponseEntity.ok(updatedCard);
    }
}