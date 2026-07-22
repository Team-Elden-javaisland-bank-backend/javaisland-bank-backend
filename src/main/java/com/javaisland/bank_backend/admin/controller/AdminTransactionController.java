package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.transaction.model.Transaction;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminTransactionController {

    private final TransactionRepository transactionRepository;

    @Data
    public static class AdminTransactionListItemDto {
        private Long id;
        private BigDecimal amount;
        private Integer typeId;
        private Integer statusId;
        private String description;
        private String sourceAccountNumber;
        private String destinationAccountNumber;
        private LocalDateTime createdAt;
    }

    @GetMapping
    public ResponseEntity<List<AdminTransactionListItemDto>> listTransactions(
            @RequestParam(required = false) Integer recentDays,
            @RequestParam(required = false) Integer typeId) {

        LocalDateTime since = recentDays != null
                ? LocalDateTime.now().minusDays(recentDays)
                : LocalDateTime.now().minusDays(30);

        PageRequest page = PageRequest.of(0, 500);

        List<Transaction> transactions;
        if (typeId != null) {
            transactions = transactionRepository.findAll(page).getContent().stream()
                    .filter(t -> t.getTypeId().equals(typeId))
                    .filter(t -> t.getCreatedAt().isAfter(since))
                    .toList();
        } else {
            transactions = transactionRepository.findAll(page).getContent().stream()
                    .filter(t -> t.getCreatedAt().isAfter(since))
                    .toList();
        }

        List<AdminTransactionListItemDto> result = transactions.stream().map(t -> {
            var dto = new AdminTransactionListItemDto();
            dto.setId(t.getId());
            dto.setAmount(t.getAmount());
            dto.setTypeId(t.getTypeId());
            dto.setStatusId(t.getStatusId());
            dto.setDescription(t.getDescription());
            dto.setSourceAccountNumber(t.getSourceAccount() != null
                    ? t.getSourceAccount().getAccountNumber() : null);
            dto.setDestinationAccountNumber(t.getDestinationAccount() != null
                    ? t.getDestinationAccount().getAccountNumber() : null);
            dto.setCreatedAt(t.getCreatedAt());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }
}
