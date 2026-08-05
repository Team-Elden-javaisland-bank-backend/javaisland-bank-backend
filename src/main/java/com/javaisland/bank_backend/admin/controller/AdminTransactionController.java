package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.admin.dto.AdminTransactionListItemDto;
import com.javaisland.bank_backend.transaction.model.Transaction;
import com.javaisland.bank_backend.transaction.model.TransactionStatus;
import com.javaisland.bank_backend.transaction.model.TransactionType;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import com.javaisland.bank_backend.transaction.repository.TransactionStatusRepository;
import com.javaisland.bank_backend.transaction.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminTransactionController {

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;

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

        Map<Integer, String> typeNames = transactionTypeRepository.findAll().stream()
                .collect(Collectors.toMap(TransactionType::getId, TransactionType::getTypeName));
        Map<Integer, String> statusNames = transactionStatusRepository.findAll().stream()
                .collect(Collectors.toMap(TransactionStatus::getId, TransactionStatus::getStatusName));

        List<AdminTransactionListItemDto> result = transactions.stream().map(t -> {
            var dto = new AdminTransactionListItemDto();
            dto.setId(t.getId());
            dto.setAmount(t.getAmount());
            dto.setTypeId(t.getTypeId());
            dto.setStatusId(t.getStatusId());
            dto.setTypeName(typeNames.get(t.getTypeId()));
            dto.setStatusName(statusNames.get(t.getStatusId()));
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
