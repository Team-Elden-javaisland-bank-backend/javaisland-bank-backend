package com.javaisland.bank_backend.card;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 📌 Inserito import per la sicurezza
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    // 🧠 ENDPOINT PER EMETTERE UNA NUOVA CARTA (Solo Dipendenti)
    @PostMapping("/issue")
    @PreAuthorize("hasRole('EMPLOYEE')") // 📌 Blocca l'accesso se non si è EMPLOYEE
    public ResponseEntity<Card> issueCard(@Valid @RequestBody CardIssueDTO dto) {
        Card newCard = cardService.issueNewCard(dto);
        return new ResponseEntity<>(newCard, HttpStatus.CREATED);
    }

    @PatchMapping("/{cardId}/status")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Card> updateStatus(
            @PathVariable Long cardId,
            @RequestParam String status) {

        Card updatedCard = cardService.updateCardStatus(cardId, status);
        return ResponseEntity.ok(updatedCard);
    }
}