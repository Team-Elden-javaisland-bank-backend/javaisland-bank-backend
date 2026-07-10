package com.javaisland.bank_backend.transaction.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.beneficiary.service.BeneficiaryService;
import com.javaisland.bank_backend.common.PageResponseDto;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.transaction.dto.TransferRequestDto;
import com.javaisland.bank_backend.transaction.dto.TransactionRequestDto;
import com.javaisland.bank_backend.transaction.dto.TransactionResponseDto;
import com.javaisland.bank_backend.transaction.model.Transaction;
import com.javaisland.bank_backend.transaction.model.TransactionStatus;
import com.javaisland.bank_backend.transaction.model.TransactionType;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import com.javaisland.bank_backend.transaction.repository.TransactionSpecifications;
import com.javaisland.bank_backend.transaction.repository.TransactionStatusRepository;
import com.javaisland.bank_backend.transaction.repository.TransactionTypeRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private static final long MAX_SEARCH_SPAN_DAYS = 30;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final UserRepository userRepository;
    private final BeneficiaryService beneficiaryService;

    @Transactional
    public Transaction transferFunds(Account source, Account destination, BigDecimal amount, String typeName, String statusName, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiBankException("Amount must be greater than zero.", "INVALID_AMOUNT");
        }
        if (source == null && destination == null) {
            throw new ApiBankException("A transaction needs at least one account.", "INVALID_TRANSACTION");
        }

        int typeId = getTypeIdOrThrow(typeName);
        int statusId = getStatusIdOrThrow(statusName);

        BigDecimal sourceBalanceAfter = null;
        BigDecimal destBalanceAfter = null;

        if (source != null) {
            if (source.getStatusId() != AccountStatus.ACTIVE) {
                throw new ApiBankException("Account " + source.getAccountNumber() + " is not active.", "INVALID_ACCOUNT_STATE");
            }
            if (source.getBalance().compareTo(amount) < 0) {
                throw new ApiBankException("Insufficient funds on account " + source.getAccountNumber(), "INSUFFICIENT_FUNDS");
            }
            source.setBalance(source.getBalance().subtract(amount));
            accountRepository.save(source);
            sourceBalanceAfter = source.getBalance();
        }

        if (destination != null) {
            if (destination.getStatusId() != AccountStatus.ACTIVE) {
                throw new ApiBankException("Account " + destination.getAccountNumber() + " is not active.", "INVALID_ACCOUNT_STATE");
            }
            destination.setBalance(destination.getBalance().add(amount));
            accountRepository.save(destination);
            destBalanceAfter = destination.getBalance();
        }

        Transaction tx = new Transaction();
        tx.setAmount(amount);
        tx.setTypeId(typeId);
        tx.setStatusId(statusId);
        tx.setDescription(description);
        tx.setSourceAccount(source);
        tx.setDestinationAccount(destination);
        tx.setSourceBalanceAfter(sourceBalanceAfter);
        tx.setDestBalanceAfter(destBalanceAfter);
        Transaction saved = transactionRepository.save(tx);

        log.info("Transaction #{} type={} amount={} source={} dest={}",
                saved.getId(), typeId, amount,
                source != null ? source.getAccountNumber() : "-",
                destination != null ? destination.getAccountNumber() : "-");

        return saved;
    }

    @Transactional
    public void deposit(Long userId, TransactionRequestDto request) {
        Account account = getAccountOrThrow(request.getAccountNumber());
        assertOwnership(userId, account);
        transferFunds(null, account, request.getAmount(), "DEPOSIT", "COMPLETED", "Deposit");
    }

    @Transactional
    public void withdraw(Long userId, TransactionRequestDto request) {
        Account account = getAccountOrThrow(request.getAccountNumber());
        assertOwnership(userId, account);
        transferFunds(account, null, request.getAmount(), "WITHDRAWAL", "COMPLETED", "Withdrawal");
    }

    @Transactional
    public TransactionResponseDto transfer(Long userId, TransferRequestDto request) {
        String destAccountNumber = request.getDestinationAccountNumber();
        if (request.getBeneficiaryId() != null) {
            destAccountNumber = beneficiaryService.resolveAccountNumber(userId, request.getBeneficiaryId());
        }
        if (destAccountNumber == null || destAccountNumber.isBlank()) {
            throw new ApiBankException("Destination account or beneficiary is required.", "INVALID_TRANSFER");
        }
        if (request.getSourceAccountNumber().equals(destAccountNumber)) {
            throw new ApiBankException("Source and destination accounts must be different.", "INVALID_TRANSFER");
        }
        Account source = getAccountOrThrow(request.getSourceAccountNumber());
        assertOwnership(userId, source);
        Account destination = getAccountOrThrow(destAccountNumber);
        String description = request.getDescription() != null ? request.getDescription() : "Transfer";
        Transaction tx = transferFunds(source, destination, request.getAmount(), "TRANSFER", "COMPLETED", description);
        return mapToResponseDto(tx);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getLast10Transactions(Long userId, String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(userId, account);
        Pageable last10 = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository
                .findBySourceAccount_IdOrDestinationAccount_IdOrderByCreatedAtDesc(account.getId(), account.getId(), last10)
                .getContent()
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<TransactionResponseDto> getAllAccountsTransactions(
            Long userId, LocalDateTime start, LocalDateTime end, int page, int size) {

        if (end.isBefore(start)) {
            throw new ApiBankException("End date must not be before start date.", "INVALID_DATE_RANGE");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_SEARCH_SPAN_DAYS) {
            throw new ApiBankException("Search interval exceeds " + MAX_SEARCH_SPAN_DAYS + " days.", "SEARCH_RANGE_TOO_WIDE");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("User not found.", "USER_NOT_FOUND"));

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        if (accounts.isEmpty()) {
            return PageResponseDto.from(Page.empty());
        }
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();

        Specification<Transaction> spec = TransactionSpecifications.forAccountIds(accountIds)
                .and(TransactionSpecifications.createdBetween(start, end));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> result = transactionRepository.findAll(spec, pageable);

        return PageResponseDto.from(result.map(this::mapToResponseDto));
    }

    private int getTypeIdOrThrow(String typeName) {
        return transactionTypeRepository.findByTypeName(typeName)
                .map(TransactionType::getId)
                .orElseThrow(() -> new ApiBankException("Transaction type '" + typeName + "' not found.", "TRANSACTION_TYPE_NOT_FOUND"));
    }

    private int getStatusIdOrThrow(String statusName) {
        return transactionStatusRepository.findByStatusName(statusName)
                .map(TransactionStatus::getId)
                .orElseThrow(() -> new ApiBankException("Transaction status '" + statusName + "' not found.", "TRANSACTION_STATUS_NOT_FOUND"));
    }

    private void assertOwnership(Long userId, Account account) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("User not found.", "USER_NOT_FOUND"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ApiBankException("Account " + account.getAccountNumber() + " does not belong to the current user.", "FORBIDDEN");
        }
    }

    private Account getAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("Account " + accountNumber + " not found.", "ACCOUNT_NOT_FOUND"));
    }

    private TransactionResponseDto mapToResponseDto(Transaction tx) {
        return TransactionResponseDto.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .typeId(tx.getTypeId())
                .statusId(tx.getStatusId())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .sourceAccountNumber(tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : null)
                .destinationAccountNumber(tx.getDestinationAccount() != null ? tx.getDestinationAccount().getAccountNumber() : null)
                .sourceBalanceAfter(tx.getSourceBalanceAfter())
                .destBalanceAfter(tx.getDestBalanceAfter())
                .build();
    }
}
