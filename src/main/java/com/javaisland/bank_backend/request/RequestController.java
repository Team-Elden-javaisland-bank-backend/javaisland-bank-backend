package com.javaisland.bank_backend.request;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    // 🧠 1. ENDPOINT PER CREARE UNA RICHIESTA GENERICA
    @PostMapping
    public ResponseEntity<BankRequest> createRequest(
            @RequestParam Long userId,
            @RequestParam String description) {

        BankRequest newRequest = requestService.createRequest(userId, description);
        return new ResponseEntity<>(newRequest, HttpStatus.CREATED);
    }

    // 🧠 2. ENDPOINT PER APPROVARE/RIFIUTARE UNA RICHIESTA (Funzione per Dipendente)
    @PatchMapping("/{requestId}/review")
    public ResponseEntity<BankRequest> reviewRequest(
            @PathVariable Long requestId,
            @RequestParam Long employeeId,
            @RequestParam String status,
            @RequestParam(required = false) String rejectionReason) {

        BankRequest reviewedRequest = requestService.reviewRequest(requestId, employeeId, status, rejectionReason);
        return ResponseEntity.ok(reviewedRequest);
    }

    // 🧠 3. ENDPOINT PER RICHIEDERE UN PRESTITO E CALCOLARE LA RATA
    @PostMapping("/loans")
    public ResponseEntity<Loan> applyForLoan(
            @RequestParam Long userId,
            @RequestParam BigDecimal amount,
            @RequestParam BigDecimal annualRate,
            @RequestParam Integer months) {

        Loan newLoan = requestService.applyForLoan(userId, amount, annualRate, months);
        return new ResponseEntity<>(newLoan, HttpStatus.CREATED);
    }
}