package com.javaisland.bank_backend.card.controller;

import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.card.dto.CardResponseDto;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.exception.ApiBankException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@PreAuthorize("hasRole('D')")
public class EmployeeCardController {

    private final CardService cardService;
    private final AccountRepository accountRepository;

    @GetMapping("/cards")
    public ResponseEntity<List<CardResponseDto>> listAllCards() {
        return ResponseEntity.ok(cardService.getAllCards());
    }

    @GetMapping("/cards/{cardId}")
    public ResponseEntity<CardResponseDto> getCardDetail(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getCardDetail(cardId));
    }

    @PutMapping("/cards/{cardId}/block")
    public ResponseEntity<Void> blockCard(@PathVariable Long cardId) {
        cardService.updateCardStatus(cardId, "BLOCKED");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/cards/{cardId}/unblock")
    public ResponseEntity<Void> unblockCard(@PathVariable Long cardId) {
        cardService.unblockCard(cardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/accounts/{accountNumber}/cards")
    public ResponseEntity<List<CardResponseDto>> listCardsByAccount(@PathVariable String accountNumber) {
        var account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("Conto " + accountNumber + " non trovato.", "ACCOUNT_NOT_FOUND"));
        return ResponseEntity.ok(cardService.getCardsByAccountId(account.getId()));
    }
}
