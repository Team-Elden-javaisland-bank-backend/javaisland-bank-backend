package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.transaction.model.Transaction;
import com.javaisland.bank_backend.transaction.model.TransactionStatus;
import com.javaisland.bank_backend.transaction.model.TransactionType;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import com.javaisland.bank_backend.transaction.repository.TransactionStatusRepository;
import com.javaisland.bank_backend.transaction.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminTransactionController {

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;

    @Data
    public static class AdminTransactionListItemDto {
        private Long id;
        private BigDecimal amount;
        private Integer typeId;
        private Integer statusId;
        private String typeName;
        private String statusName;
        private String description;
        private String sourceAccountNumber;
        private String destinationAccountNumber;
        private OffsetDateTime createdAt;
    }

    @GetMapping
    public ResponseEntity<List<AdminTransactionListItemDto>> listTransactions(
            @RequestParam(required = false) Integer recentDays,
            @RequestParam(required = false) Integer typeId) {

        OffsetDateTime since = recentDays != null
                ? OffsetDateTime.now().minusDays(recentDays)
                : OffsetDateTime.now().minusDays(30);

        List<Transaction> transactions;
        if (typeId != null) {
            transactions = transactionRepository.findByTypeIdAndCreatedAtAfter(typeId, since, PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            transactions = transactionRepository.findByCreatedAtAfter(since, PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        List<AdminTransactionListItemDto> result = transactions.stream().map(t -> {
            var dto = new AdminTransactionListItemDto();
            dto.setId(t.getId());
            dto.setAmount(t.getAmount());
            dto.setTypeId(t.getTypeId());
            dto.setStatusId(t.getStatusId());
            dto.setTypeName(transactionTypeRepository.findById(t.getTypeId())
                    .map(TransactionType::getTypeName).orElse(null));
            dto.setStatusName(transactionStatusRepository.findById(t.getStatusId())
                    .map(TransactionStatus::getStatusName).orElse(null));
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
