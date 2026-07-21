package com.javaisland.bank_backend.transaction.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.service.AccountLimitService;
import com.javaisland.bank_backend.beneficiary.service.BeneficiaryService;
import com.javaisland.bank_backend.common.PageResponseDto;
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
    private final NotificationService notificationService;

    @Transactional
    public Transaction transferFunds(Account source, Account destination, BigDecimal amount, String typeName, String statusName, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiBankException("L'importo deve essere maggiore di zero.", "INVALID_AMOUNT");
        }
        if (source == null && destination == null) {
            throw new ApiBankException("Una transazione richiede almeno un conto.", "INVALID_TRANSACTION");
        }

        int typeId = getTypeIdOrThrow(typeName);
        int statusId = getStatusIdOrThrow(statusName);

        BigDecimal sourceBalanceAfter = null;
        BigDecimal destBalanceAfter = null;

        if (source != null) {
            if (source.getStatusId() != AccountStatus.ACTIVE) {
                throw new ApiBankException(
                    "Il conto " + source.getAccountNumber() + " non è attivo. Prelievo non possibile.", "INVALID_ACCOUNT_STATE");
            }
            if (source.getBalance().compareTo(amount) < 0) {
                throw new ApiBankException(
                    "Fondi insufficienti sul conto " + source.getAccountNumber()
                    + ". Disponibili: €" + source.getBalance() + ", richiesti: €" + amount + ".",
                    "INSUFFICIENT_FUNDS");
            }
            source.setBalance(source.getBalance().subtract(amount));
            accountRepository.save(source);
            sourceBalanceAfter = source.getBalance();
        }

        if (destination != null) {
            if (destination.getStatusId() != AccountStatus.ACTIVE) {
                throw new ApiBankException("Il conto " + destination.getAccountNumber() + " non è attivo.", "INVALID_ACCOUNT_STATE");
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

        if (destination != null && source != null) {
            notificationService.send(destination.getUser().getId(), "TRANSFER", "Ricevuto bonifico di €" + amount + " da " + source.getAccountNumber() + ".");
        }

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
        transferFunds(null, account, request.getAmount(), "DEPOSIT", "COMPLETED", "Deposito");
        notificationService.send(userId, "DEPOSIT", "Deposito di €" + request.getAmount() + " sul conto " + request.getAccountNumber() + " completato.");
    }

    private static final BigDecimal MIN_WITHDRAWAL = new BigDecimal("10");

    @Transactional
    public void withdraw(Long userId, TransactionRequestDto request) {
        BigDecimal amount = request.getAmount();

        if (amount.compareTo(MIN_WITHDRAWAL) < 0) {
            throw new ApiBankException(
                "L'importo del prelievo €" + amount + " è inferiore al minimo di €" + MIN_WITHDRAWAL + ".",
                "MINIMUM_WITHDRAWAL");
        }

        Account account = getAccountOrThrow(request.getAccountNumber());
        assertOwnership(userId, account);
        accountLimitService.validateWithdrawal(account, amount);
        transferFunds(account, null, amount, "WITHDRAWAL", "COMPLETED", "Prelievo");
        notificationService.send(userId, "WITHDRAWAL", "Prelievo di €" + amount + " dal conto " + request.getAccountNumber() + " completato.");
    }

    @Transactional
    public TransactionResponseDto transfer(Long userId, TransferRequestDto request) {
        if (request.getAmount().compareTo(new BigDecimal("1")) < 0) {
            throw new ApiBankException("L'importo minimo per il bonifico è €1,00.", "MINIMUM_TRANSFER");
        }
        String destAccountNumber = request.getDestinationAccountNumber();
        if (request.getBeneficiaryId() != null) {
            destAccountNumber = beneficiaryService.resolveAccountNumber(userId, request.getBeneficiaryId());
        }
        if (destAccountNumber == null || destAccountNumber.isBlank()) {
            throw new ApiBankException("Il conto di destinazione o il beneficiario è obbligatorio.", "INVALID_TRANSFER");
        }
        if (request.getSourceAccountNumber().equals(destAccountNumber)) {
            throw new ApiBankException("I conti sorgente e destinazione devono essere diversi.", "INVALID_TRANSFER");
        }
        Account source = getAccountOrThrow(request.getSourceAccountNumber());
        assertOwnership(userId, source);
        Account destination = getAccountOrThrow(destAccountNumber);

        boolean isInstant = Boolean.TRUE.equals(request.getIsInstant());
        accountLimitService.validateTransfer(source, request.getAmount(), isInstant);

        String typeName = isInstant ? "INSTANT_TRANSFER" : "TRANSFER";
        String description = request.getDescription() != null ? request.getDescription() : (isInstant ? "Bonifico Istantaneo" : "Bonifico Programmato");

        if (isInstant) {
            Transaction tx = transferFunds(source, destination, request.getAmount(), typeName, "COMPLETED", description);
            notificationService.send(userId, "TRANSFER", "Bonifico di €" + request.getAmount() + " a " + destination.getAccountNumber() + " completato.");
            return mapToResponseDto(tx);
        }

        LocalDate scheduledDate = request.getScheduledDate();
        if (scheduledDate == null) {
            throw new ApiBankException("La data programmata è obbligatoria per i bonifici normali.", "MISSING_SCHEDULED_DATE");
        }
        if (!scheduledDate.isAfter(LocalDate.now())) {
            throw new ApiBankException("La data programmata deve essere almeno domani.", "INVALID_SCHEDULED_DATE");
        }
        if (ChronoUnit.DAYS.between(LocalDate.now(), scheduledDate) > MAX_SCHEDULE_DAYS) {
            throw new ApiBankException("La data programmata non può essere più di " + MAX_SCHEDULE_DAYS + " giorni da oggi.", "SCHEDULE_TOO_FAR");
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

        notificationService.send(userId, "SCHEDULED_TRANSFER", "Bonifico programmato di €" + request.getAmount() + " a " + destination.getAccountNumber() + " per il " + scheduledDate + ".");

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
            throw new ApiBankException("La data di fine non può essere precedente alla data di inizio.", "INVALID_DATE_RANGE");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_SEARCH_SPAN_DAYS) {
            throw new ApiBankException("L'intervallo di ricerca supera i " + MAX_SEARCH_SPAN_DAYS + " giorni.", "SEARCH_RANGE_TOO_WIDE");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

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
                    tx.setDescription(tx.getDescription() + " - Conto sorgente non attivo");
                    transactionRepository.save(tx);
                    log.warn("Scheduled transfer #{} failed: source account not active", tx.getId());
                    continue;
                }

                if (destination != null && destination.getStatusId() != AccountStatus.ACTIVE) {
                    tx.setStatusId(getStatusIdOrThrow("FAILED"));
                    tx.setDescription(tx.getDescription() + " - Conto destinazione non attivo");
                    transactionRepository.save(tx);
                    log.warn("Scheduled transfer #{} failed: destination account not active", tx.getId());
                    continue;
                }

                if (source != null && source.getBalance().compareTo(tx.getAmount()) < 0) {
                    tx.setStatusId(getStatusIdOrThrow("FAILED"));
                    tx.setDescription(tx.getDescription() + " - Fondi insufficienti");
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

                if (source != null) {
                    notificationService.send(source.getUser().getId(), "TRANSFER", "Bonifico programmato di €" + tx.getAmount() + " eseguito verso " + destination.getAccountNumber() + ".");
                }
                if (destination != null) {
                    notificationService.send(destination.getUser().getId(), "TRANSFER", "Ricevuto bonifico di €" + tx.getAmount() + " da " + source.getAccountNumber() + ".");
                }

                log.info("Scheduled transfer #{} executed successfully", tx.getId());
            } catch (Exception e) {
                log.error("Error executing scheduled transfer #{}: {}", tx.getId(), e.getMessage());
                tx.setStatusId(getStatusIdOrThrow("FAILED"));
                tx.setDescription(tx.getDescription() + " - Errore di esecuzione: " + e.getMessage());
                transactionRepository.save(tx);
            }
        }
    }

    @Transactional
    public void cancelPendingTransaction(Long userId, Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ApiBankException("Transazione non trovata.", "TRANSACTION_NOT_FOUND"));

        int pendingStatusId = getStatusIdOrThrow("PENDING");
        if (tx.getStatusId() != pendingStatusId) {
            throw new ApiBankException("Solo le transazioni in sospeso possono essere annullate.", "INVALID_TRANSACTION_STATE");
        }

        if (tx.getSourceAccount() != null) {
            assertOwnership(userId, tx.getSourceAccount());
        }

        tx.setStatusId(getStatusIdOrThrow("CANCELLED"));
        tx.setDescription(tx.getDescription() + " - Annullato dall'utente");
        transactionRepository.save(tx);

        notificationService.send(userId, "TRANSFER", "Transazione #" + transactionId + " annullata.");

        log.info("Transaction #{} cancelled by user", transactionId);
    }

    private int getTypeIdOrThrow(String typeName) {
        return transactionTypeRepository.findByTypeName(typeName)
                .map(TransactionType::getId)
                .orElseThrow(() -> new ApiBankException("Tipo transazione '" + typeName + "' non trovato.", "TRANSACTION_TYPE_NOT_FOUND"));
    }

    private int getStatusIdOrThrow(String statusName) {
        return transactionStatusRepository.findByStatusName(statusName)
                .map(TransactionStatus::getId)
                .orElseThrow(() -> new ApiBankException("Stato transazione '" + statusName + "' non trovato.", "TRANSACTION_STATUS_NOT_FOUND"));
    }

    private void assertOwnership(Long userId, Account account) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ApiBankException("Il conto " + account.getAccountNumber() + " non appartiene all'utente corrente.", "FORBIDDEN");
        }
    }

    private Account getAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("Conto " + accountNumber + " non trovato.", "ACCOUNT_NOT_FOUND"));
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
