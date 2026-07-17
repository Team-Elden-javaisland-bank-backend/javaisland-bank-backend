package com.javaisland.bank_backend.transaction.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.service.AccountLimitService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private static final long MAX_SEARCH_SPAN_DAYS = 365;
    private static final long MAX_SCHEDULE_DAYS = 30;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final UserRepository userRepository;
    private final BeneficiaryService beneficiaryService;
    private final AccountLimitService accountLimitService;

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
                throw new ApiBankException(
                    "Account " + source.getAccountNumber() + " is not active. Withdrawal not possible.", "INVALID_ACCOUNT_STATE");
            }
            if (source.getBalance().compareTo(amount) < 0) {
                throw new ApiBankException(
                    "Insufficient funds on account " + source.getAccountNumber()
                    + ". Available: €" + source.getBalance() + ", requested: €" + amount + ".",
                    "INSUFFICIENT_FUNDS");
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

    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("10");

    @Transactional
    public void withdraw(Long userId, TransactionRequestDto request) {
        BigDecimal amount = request.getAmount();

        if (amount.compareTo(MIN_WITHDRAWAL) < 0) {
            throw new ApiBankException(
                "Withdrawal amount €" + amount + " is below the minimum of €" + MIN_WITHDRAWAL + ".",
                "MINIMUM_WITHDRAWAL");
        }

        Account account = getAccountOrThrow(request.getAccountNumber());
        assertOwnership(userId, account);
        accountLimitService.validateWithdrawal(account, amount);
        transferFunds(account, null, amount, "WITHDRAWAL", "COMPLETED", "Withdrawal");
    }

    @Transactional
    public TransactionResponseDto transfer(Long userId, TransferRequestDto request) {
        if (request.getAmount().compareTo(new BigDecimal("1")) < 0) {
            throw new ApiBankException("Minimum transfer amount is €1.00.", "MINIMUM_TRANSFER");
        }
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

        boolean isInstant = Boolean.TRUE.equals(request.getIsInstant());
        accountLimitService.validateTransfer(source, request.getAmount(), isInstant);

        String typeName = isInstant ? "INSTANT_TRANSFER" : "TRANSFER";
        String description = request.getDescription() != null ? request.getDescription() : (isInstant ? "Instant Transfer" : "Scheduled Transfer");

        if (isInstant) {
            Transaction tx = transferFunds(source, destination, request.getAmount(), typeName, "COMPLETED", description);
            return mapToResponseDto(tx);
        }

        LocalDate scheduledDate = request.getScheduledDate();
        if (scheduledDate == null) {
            throw new ApiBankException("Scheduled date is required for normal transfers.", "MISSING_SCHEDULED_DATE");
        }
        if (!scheduledDate.isAfter(LocalDate.now())) {
            throw new ApiBankException("Scheduled date must be at least tomorrow.", "INVALID_SCHEDULED_DATE");
        }
        if (ChronoUnit.DAYS.between(LocalDate.now(), scheduledDate) > MAX_SCHEDULE_DAYS) {
            throw new ApiBankException("Scheduled date cannot be more than " + MAX_SCHEDULE_DAYS + " days from today.", "SCHEDULE_TOO_FAR");
        }

        int typeId = getTypeIdOrThrow(typeName);
        int statusId = getStatusIdOrThrow("PENDING");

        Transaction tx = new Transaction();
        tx.setAmount(request.getAmount());
        tx.setTypeId(typeId);
        tx.setStatusId(statusId);
        tx.setDescription(description);
        tx.setSourceAccount(source);
        tx.setDestinationAccount(destination);
        tx.setScheduledDate(scheduledDate.atStartOfDay());
        Transaction saved = transactionRepository.save(tx);

        log.info("Scheduled transaction #{} type={} amount={} source={} dest={} scheduledDate={}",
                saved.getId(), typeName, request.getAmount(),
                source.getAccountNumber(), destination.getAccountNumber(), scheduledDate);

        return mapToResponseDto(saved);
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

    @Transactional
    public void executePendingTransfers() {
        int pendingStatusId = getStatusIdOrThrow("PENDING");
        List<Transaction> pendingTransactions = transactionRepository
                .findByStatusIdAndScheduledDateLessThanEqual(pendingStatusId, LocalDateTime.now());

        for (Transaction tx : pendingTransactions) {
            try {
                Account source = tx.getSourceAccount();
                Account destination = tx.getDestinationAccount();

                if (source != null && source.getStatusId() != AccountStatus.ACTIVE) {
                    tx.setStatusId(getStatusIdOrThrow("FAILED"));
                    tx.setDescription(tx.getDescription() + " - Source account not active");
                    transactionRepository.save(tx);
                    log.warn("Scheduled transfer #{} failed: source account not active", tx.getId());
                    continue;
                }

                if (destination != null && destination.getStatusId() != AccountStatus.ACTIVE) {
                    tx.setStatusId(getStatusIdOrThrow("FAILED"));
                    tx.setDescription(tx.getDescription() + " - Destination account not active");
                    transactionRepository.save(tx);
                    log.warn("Scheduled transfer #{} failed: destination account not active", tx.getId());
                    continue;
                }

                if (source != null && source.getBalance().compareTo(tx.getAmount()) < 0) {
                    tx.setStatusId(getStatusIdOrThrow("FAILED"));
                    tx.setDescription(tx.getDescription() + " - Insufficient funds");
                    transactionRepository.save(tx);
                    log.warn("Scheduled transfer #{} failed: insufficient funds", tx.getId());
                    continue;
                }

                if (source != null) {
                    source.setBalance(source.getBalance().subtract(tx.getAmount()));
                    accountRepository.save(source);
                    tx.setSourceBalanceAfter(source.getBalance());
                }

                if (destination != null) {
                    destination.setBalance(destination.getBalance().add(tx.getAmount()));
                    accountRepository.save(destination);
                    tx.setDestBalanceAfter(destination.getBalance());
                }

                tx.setStatusId(getStatusIdOrThrow("COMPLETED"));
                transactionRepository.save(tx);

                log.info("Scheduled transfer #{} executed successfully", tx.getId());
            } catch (Exception e) {
                log.error("Error executing scheduled transfer #{}: {}", tx.getId(), e.getMessage());
                tx.setStatusId(getStatusIdOrThrow("FAILED"));
                tx.setDescription(tx.getDescription() + " - Execution error: " + e.getMessage());
                transactionRepository.save(tx);
            }
        }
    }

    @Transactional
    public void cancelPendingTransaction(Long userId, Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiBankException("Transaction not found.", "TRANSACTION_NOT_FOUND"));

        int pendingStatusId = getStatusIdOrThrow("PENDING");
        if (tx.getStatusId() != pendingStatusId) {
            throw new ApiBankException("Only pending transactions can be cancelled.", "INVALID_TRANSACTION_STATE");
        }

        if (tx.getSourceAccount() != null) {
            assertOwnership(userId, tx.getSourceAccount());
        }

        tx.setStatusId(getStatusIdOrThrow("CANCELLED"));
        tx.setDescription(tx.getDescription() + " - Cancelled by user");
        transactionRepository.save(tx);

        log.info("Transaction #{} cancelled by user", transactionId);
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
        TransactionType txType = transactionTypeRepository.findById(tx.getTypeId()).orElse(null);
        TransactionStatus txStatus = transactionStatusRepository.findById(tx.getStatusId()).orElse(null);

        String sourceUserName = null;
        if (tx.getSourceAccount() != null && tx.getSourceAccount().getUser() != null) {
            var user = tx.getSourceAccount().getUser();
            sourceUserName = user.getFirstName() + " " + user.getLastName();
        }

        String destinationUserName = null;
        if (tx.getDestinationAccount() != null && tx.getDestinationAccount().getUser() != null) {
            var user = tx.getDestinationAccount().getUser();
            destinationUserName = user.getFirstName() + " " + user.getLastName();
        }

        return TransactionResponseDto.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .typeId(tx.getTypeId())
                .statusId(tx.getStatusId())
                .typeName(txType != null ? txType.getTypeName() : null)
                .statusName(txStatus != null ? txStatus.getStatusName() : null)
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .scheduledDate(tx.getScheduledDate())
                .sourceAccountNumber(tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : null)
                .destinationAccountNumber(tx.getDestinationAccount() != null ? tx.getDestinationAccount().getAccountNumber() : null)
                .sourceUserName(sourceUserName)
                .destinationUserName(destinationUserName)
                .sourceBalanceAfter(tx.getSourceBalanceAfter())
                .destBalanceAfter(tx.getDestBalanceAfter())
                .build();
    }
}
