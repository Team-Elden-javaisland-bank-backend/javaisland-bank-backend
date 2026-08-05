package com.javaisland.bank_backend.transaction.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.service.AccountLimitService;
import com.javaisland.bank_backend.beneficiary.service.BeneficiaryService;
import com.javaisland.bank_backend.common.PageResponseDto;
import com.javaisland.bank_backend.common.SecurityUtil;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
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
import com.javaisland.bank_backend.user.service.UserPinService;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    private final NotificationService notificationService;
    private final SecurityUtil securityUtil;
    private final UserPinService userPinService;

    @Transactional
    public Transaction transferFunds(Account source, Account destination, BigDecimal amount, String typeName, String statusName, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiBankException("INVALID_AMOUNT", "INVALID_AMOUNT");
        }
        if (source == null && destination == null) {
            throw new ApiBankException("INVALID_TRANSACTION", "INVALID_TRANSACTION");
        }

        int typeId = getTypeIdOrThrow(typeName);
        int statusId = getStatusIdOrThrow(statusName);

        BigDecimal sourceBalanceAfter = null;
        BigDecimal destBalanceAfter = null;

        if (source != null) {
            if (source.getStatusId() != AccountStatus.ACTIVE) {
                throw new ApiBankException(
                    "INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
            }
            if (source.getBalance().compareTo(amount) < 0) {
                throw new ApiBankException(
                    "INSUFFICIENT_FUNDS", "INSUFFICIENT_FUNDS");
            }
            source.setBalance(source.getBalance().subtract(amount));
            sourceBalanceAfter = source.getBalance();
        }

        if (destination != null) {
            if (destination.getStatusId() != AccountStatus.ACTIVE) {
                throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
            }
            destination.setBalance(destination.getBalance().add(amount));
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

        if (source != null) {
            accountRepository.save(source);
        }
        if (destination != null) {
            accountRepository.save(destination);
        }

        if (destination != null && source != null && destination.getUser() != null && source.getUser() != null) {
            notificationService.send(destination.getUser().getId(), "TRANSFER", "Transfer of €" + amount + " received from " + source.getAccountNumber() + ".", "NOTIF_TRANSFER_RECEIVED", "[\"" + amount + "\", \"" + source.getAccountNumber() + "\"]");
        }

        log.info("Transaction #{} type={} amount={} source={} dest={}",
                saved.getId(), typeId, amount,
                source != null ? source.getAccountNumber() : "-",
                destination != null ? destination.getAccountNumber() : "-");

        return saved;
    }

    @Transactional
    public TransactionResponseDto deposit(Long userId, TransactionRequestDto request) {
        Account account = getAccountOrThrowForUpdate(request.getAccountNumber());
        assertOwnership(userId, account);
        Transaction tx = transferFunds(null, account, request.getAmount(), "DEPOSIT", "COMPLETED", "Deposito");
        notificationService.send(userId, "DEPOSIT", "Deposit of €" + request.getAmount() + " to account " + request.getAccountNumber() + " completed.", "NOTIF_DEPOSIT_COMPLETED", "[\"" + request.getAmount() + "\", \"" + request.getAccountNumber() + "\"]");
        return mapToResponseDto(tx);
    }

    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("10");
    private static final BigDecimal ATM_MULTIPLE = new BigDecimal("10");

    @Transactional
    public TransactionResponseDto withdraw(Long userId, TransactionRequestDto request) {
        BigDecimal amount = request.getAmount();

        requireValidPin(userId, request.getPin());

        if (amount.compareTo(MIN_WITHDRAWAL) < 0) {
            throw new ApiBankException(
                "MINIMUM_WITHDRAWAL", "MINIMUM_WITHDRAWAL");
        }

        if (amount.remainder(ATM_MULTIPLE).compareTo(BigDecimal.ZERO) != 0) {
            throw new ApiBankException(
                "INVALID_ATM_AMOUNT", "INVALID_ATM_AMOUNT");
        }

        Account account = getAccountOrThrowForUpdate(request.getAccountNumber());
        assertOwnership(userId, account);
        accountLimitService.validateWithdrawal(account, amount);
        Transaction tx = transferFunds(account, null, amount, "WITHDRAWAL", "COMPLETED", "Prelievo");
        notificationService.send(userId, "WITHDRAWAL", "Withdrawal of €" + amount + " from account " + request.getAccountNumber() + " completed.", "NOTIF_WITHDRAWAL_COMPLETED", "[\"" + amount + "\", \"" + request.getAccountNumber() + "\"]");
        return mapToResponseDto(tx);
    }

    @Transactional
    public TransactionResponseDto transfer(Long userId, TransferRequestDto request) {
        requireValidPin(userId, request.getPin());
        String destAccountNumber = request.getDestinationAccountNumber();
        if (request.getBeneficiaryId() != null) {
            destAccountNumber = beneficiaryService.resolveAccountNumber(userId, request.getBeneficiaryId());
        }
        if (destAccountNumber == null || destAccountNumber.isBlank()) {
            throw new ApiBankException("INVALID_TRANSFER", "INVALID_TRANSFER");
        }
        if (request.getSourceAccountNumber().equals(destAccountNumber)) {
            throw new ApiBankException("INVALID_TRANSFER", "INVALID_TRANSFER");
        }
        String sourceNumber = request.getSourceAccountNumber();
        String destNumber = destAccountNumber;

        String lockFirst = sourceNumber.compareTo(destNumber) <= 0 ? sourceNumber : destNumber;
        String lockSecond = sourceNumber.compareTo(destNumber) <= 0 ? destNumber : sourceNumber;

        Account firstLocked = getAccountOrThrowForUpdate(lockFirst);
        Account secondLocked = getAccountOrThrowForUpdate(lockSecond);

        Account source = sourceNumber.equals(lockFirst) ? firstLocked : secondLocked;
        Account destination = sourceNumber.equals(lockFirst) ? secondLocked : firstLocked;

        assertOwnership(userId, source);

        boolean isSameUser = source.getUser().getId().equals(destination.getUser().getId());
        boolean isInstant = isSameUser || Boolean.TRUE.equals(request.getIsInstant());
        if (!isSameUser && request.getAmount().compareTo(new BigDecimal("1")) < 0) {
            throw new ApiBankException("MINIMUM_TRANSFER", "MINIMUM_TRANSFER");
        }
        if (!isSameUser) {
            accountLimitService.validateTransfer(source, request.getAmount(), isInstant);
        }

        String typeName = isInstant ? "INSTANT_TRANSFER" : "TRANSFER";
        String description = request.getDescription() != null ? request.getDescription() : (isInstant ? "Bonifico Istantaneo" : "Bonifico Programmato");

        if (isInstant) {
            Transaction tx = transferFunds(source, destination, request.getAmount(), typeName, "COMPLETED", description);
            notificationService.send(userId, "TRANSFER", "Transfer of €" + request.getAmount() + " to " + destination.getAccountNumber() + " completed.", "NOTIF_TRANSFER_COMPLETED", "[\"" + request.getAmount() + "\", \"" + destination.getAccountNumber() + "\"]");
            return mapToResponseDto(tx);
        }

        LocalDate scheduledDate = request.getScheduledDate();
        if (scheduledDate == null) {
            throw new ApiBankException("MISSING_SCHEDULED_DATE", "MISSING_SCHEDULED_DATE");
        }
        if (!scheduledDate.isAfter(LocalDate.now())) {
            throw new ApiBankException("INVALID_SCHEDULED_DATE", "INVALID_SCHEDULED_DATE");
        }
        if (ChronoUnit.DAYS.between(LocalDate.now(), scheduledDate) > MAX_SCHEDULE_DAYS) {
            throw new ApiBankException("SCHEDULE_TOO_FAR", "SCHEDULE_TOO_FAR");
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
        tx.setScheduledDate(scheduledDate.atStartOfDay(ZoneId.of("Europe/Rome")).toOffsetDateTime());
        Transaction saved = transactionRepository.save(tx);

        notificationService.send(userId, "SCHEDULED_TRANSFER", "Scheduled transfer of €" + request.getAmount() + " to " + destination.getAccountNumber() + " for " + scheduledDate + ".", "NOTIF_SCHEDULED_TRANSFER_CREATED", "[\"" + request.getAmount() + "\", \"" + destination.getAccountNumber() + "\", \"" + scheduledDate + "\"]");

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
            Long userId, OffsetDateTime start, OffsetDateTime end, int page, int size,
            String accountNumber) {

        if (end.isBefore(start)) {
            throw new ApiBankException("INVALID_DATE_RANGE", "INVALID_DATE_RANGE");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_SEARCH_SPAN_DAYS) {
            throw new ApiBankException("SEARCH_RANGE_TOO_WIDE", "SEARCH_RANGE_TOO_WIDE");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        if (accounts.isEmpty()) {
            return PageResponseDto.from(Page.empty());
        }
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();

        if (accountNumber != null && !accountNumber.isBlank()) {
            Account account = getAccountOrThrow(accountNumber);
            assertOwnership(userId, account);
            accountIds = List.of(account.getId());
        }

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
                .findByStatusIdAndScheduledDateLessThanEqual(pendingStatusId, OffsetDateTime.now());

        for (Transaction tx : pendingTransactions) {
            try {
                Long srcId = tx.getSourceAccount() != null ? tx.getSourceAccount().getId() : null;
                Long dstId = tx.getDestinationAccount() != null ? tx.getDestinationAccount().getId() : null;

                Account source = null;
                Account destination = null;

                if (srcId != null && dstId != null && !srcId.equals(dstId)) {
                    long lockLo = Math.min(srcId, dstId);
                    long lockHi = Math.max(srcId, dstId);
                    Account lo = accountRepository.findByIdForUpdate(lockLo).orElse(null);
                    Account hi = accountRepository.findByIdForUpdate(lockHi).orElse(null);
                    source = srcId == lockLo ? lo : hi;
                    destination = srcId == lockLo ? hi : lo;
                } else if (srcId != null && dstId != null) {
                    source = destination = accountRepository.findByIdForUpdate(srcId).orElse(null);
                } else if (srcId != null) {
                    source = accountRepository.findByIdForUpdate(srcId).orElse(null);
                } else if (dstId != null) {
                    destination = accountRepository.findByIdForUpdate(dstId).orElse(null);
                }

                if (srcId != null && source == null) {
                    failTransaction(tx, "Source account not found");
                    continue;
                }

                if (dstId != null && destination == null) {
                    failTransaction(tx, "Destination account not found");
                    continue;
                }

                if (source != null && source.getStatusId() != AccountStatus.ACTIVE) {
                    failTransaction(tx, "Source account not active");
                    continue;
                }

                if (destination != null && destination.getStatusId() != AccountStatus.ACTIVE) {
                    failTransaction(tx, "Destination account not active");
                    continue;
                }

                if (source != null && source.getBalance().compareTo(tx.getAmount()) < 0) {
                    failTransaction(tx, "Insufficient funds");
                    continue;
                }

                if (source != null && destination != null) {
                    boolean sameUser = source.getUser() != null && destination.getUser() != null
                            && source.getUser().getId().equals(destination.getUser().getId());
                    if (!sameUser) {
                        try {
                            accountLimitService.validateTransfer(source, tx.getAmount(), false);
                        } catch (ApiBankException e) {
                            failTransaction(tx, "Limit exceeded");
                            continue;
                        }
                    }
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

                if (source != null && source.getUser() != null) {
                    String destNum = destination != null ? destination.getAccountNumber() : "-";
                    notificationService.send(source.getUser().getId(), "TRANSFER", "Scheduled transfer of €" + tx.getAmount() + " executed to " + destNum + ".", "NOTIF_SCHEDULED_TRANSFER_EXECUTED", "[\"" + tx.getAmount() + "\", \"" + destNum + "\"]");
                }
                if (destination != null && destination.getUser() != null) {
                    String srcNum = source != null ? source.getAccountNumber() : "-";
                    notificationService.send(destination.getUser().getId(), "TRANSFER", "Transfer of €" + tx.getAmount() + " received from " + srcNum + ".", "NOTIF_TRANSFER_RECEIVED", "[\"" + tx.getAmount() + "\", \"" + srcNum + "\"]");
                }

                log.info("Scheduled transfer #{} executed successfully", tx.getId());
            } catch (Exception e) {
                log.error("Error executing scheduled transfer #{}: {}", tx.getId(), e.getMessage());
                failTransaction(tx, "Execution error: " + e.getMessage());
            }
        }
    }

    private void requireValidPin(Long userId, String pin) {
        if (pin == null || pin.isBlank() || !userPinService.verifyPin(userId, pin)) {
            throw new ApiBankException("INVALID_PIN", "INVALID_PIN");
        }
    }

    private void failTransaction(Transaction tx, String reason) {
        tx.setStatusId(getStatusIdOrThrow("FAILED"));
        tx.setDescription(tx.getDescription() + " - " + reason);
        transactionRepository.save(tx);
        log.warn("Scheduled transfer #{} failed: {}", tx.getId(), reason);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getScheduledTransfers(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        if (accounts.isEmpty()) {
            return List.of();
        }
        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        int scheduledStatusId = getStatusIdOrThrow("PENDING");

        return transactionRepository.findScheduledByAccountIds(scheduledStatusId, accountIds).stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Transactional
    public void cancelPendingTransaction(Long userId, Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiBankException("TRANSACTION_NOT_FOUND", "TRANSACTION_NOT_FOUND"));

        int pendingStatusId = getStatusIdOrThrow("PENDING");
        if (tx.getStatusId() != pendingStatusId) {
            throw new ApiBankException("INVALID_TRANSACTION_STATE", "INVALID_TRANSACTION_STATE");
        }

        if (tx.getSourceAccount() != null) {
            assertOwnership(userId, tx.getSourceAccount());
        }

        tx.setStatusId(getStatusIdOrThrow("CANCELLED"));
        tx.setDescription(tx.getDescription() + " - TRANSACTION_CANCELLED_BY_USER");
        transactionRepository.save(tx);

        notificationService.send(userId, "TRANSFER", "Transaction #" + transactionId + " cancelled.", "NOTIF_TRANSACTION_CANCELLED", "[\"" + transactionId + "\"]");

        log.info("Transaction #{} cancelled by user", transactionId);
    }

    private int getTypeIdOrThrow(String typeName) {
        return transactionTypeRepository.findByTypeName(typeName)
                .map(TransactionType::getId)
                .orElseThrow(() -> new ApiBankException("TRANSACTION_TYPE_NOT_FOUND", "TRANSACTION_TYPE_NOT_FOUND"));
    }

    private int getStatusIdOrThrow(String statusName) {
        return transactionStatusRepository.findByStatusName(statusName)
                .map(TransactionStatus::getId)
                .orElseThrow(() -> new ApiBankException("TRANSACTION_STATUS_NOT_FOUND", "TRANSACTION_STATUS_NOT_FOUND"));
    }

    private void assertOwnership(Long userId, Account account) {
        securityUtil.assertOwnership(account, userId);
    }

    private Account getAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));
    }

    private Account getAccountOrThrowForUpdate(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));
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
