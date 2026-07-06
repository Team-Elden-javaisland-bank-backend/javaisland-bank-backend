package com.javaisland.bank_backend.card;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    // 🧠 ENDPOINT PER EMETTERE UNA NUOVA CARTA
    @PostMapping("/issue")
    public ResponseEntity<Card> issueCard(
            @RequestParam Long accountId,
            @RequestParam String holderName,
            @RequestParam CardType cardType) { // Cambiato da Integer a CardType

        Card newCard = cardService.issueNewCard(accountId, holderName, cardType);
        return new ResponseEntity<>(newCard, HttpStatus.CREATED);
    }

    // 🧠 ENDPOINT PER CAMBIARE STATO (Attivazione o Blocco)
    @PatchMapping("/{cardId}/status")
    public ResponseEntity<Card> updateStatus(
            @PathVariable Long cardId,
            @RequestParam CardStatus status) { // Cambiato da Integer a CardStatus

        Card updatedCard = cardService.updateCardStatus(cardId, status);
        return ResponseEntity.ok(updatedCard);
    }
}