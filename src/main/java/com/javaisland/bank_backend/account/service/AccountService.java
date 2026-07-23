package com.javaisland.bank_backend.account.service;

import com.javaisland.bank_backend.account.dto.AccountHolderDto;
import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import com.javaisland.bank_backend.account.dto.CloseAccountRequestDto;
import com.javaisland.bank_backend.account.dto.EmployeeUserDetailDto;
import com.javaisland.bank_backend.account.dto.OpenAccountRequestDto;
import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.audit.service.AuditLogService;
import com.javaisland.bank_backend.card.repository.CardRepository;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.transaction.service.TransactionService;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.javaisland.bank_backend.account.dto.MonthlySummaryDto;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final int MAX_IBAN_GENERATION_ATTEMPTS = 5;
    private static final int MAX_ACCOUNTS_PER_USER = 3;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CardService cardService;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRED)
    public Account createInitialAccountForUser(User user) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));
        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setStatusId(AccountStatus.INACTIVE);
        account.setUser(managedUser);
        Account saved = accountRepository.save(account);
        log.info("Created initial account {} (INACTIVE) for user id={}", saved.getAccountNumber(), managedUser.getId());
        return saved;
    }

    @Transactional
    public void activateInitialAccountForUser(Long userId) {
        boolean userHasActiveAccount = accountRepository.findByUserId(userId).stream()
                .anyMatch(a -> a.getStatusId() == AccountStatus.ACTIVE);
        if (userHasActiveAccount) {
            throw new ApiBankException(
                    "L'utente ha già un conto attivo. Usa l'attivazione conto da impiegato.", "INVALID_STATE");
        }
        Account account = accountRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatusId() == AccountStatus.INACTIVE)
                .findFirst()
                .orElseThrow(() -> new ApiBankException(
                        "Nessun conto in attesa trovato per questa registrazione.", "ACCOUNT_NOT_FOUND"));
        account.setStatusId(AccountStatus.ACTIVE);
        accountRepository.save(account);
        log.info("Account {} activated as part of registration validation for user id={}", account.getAccountNumber(), userId);
    }

    @Transactional
    public void activateAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.INACTIVE) {
            throw new ApiBankException("Il conto " + accountNumber + " non è in attesa di attivazione.", "INVALID_ACCOUNT_STATE");
        }
        boolean userHasActiveAccount = accountRepository.findByUserId(account.getUser().getId()).stream()
                .anyMatch(a -> a.getStatusId() == AccountStatus.ACTIVE);
        if (!userHasActiveAccount) {
            throw new ApiBankException("Il conto " + accountNumber + " appartiene a una registrazione in corso. Usa l'attivazione registrazione.", "REGISTRATION_ACCOUNT");
        }
        account.setStatusId(AccountStatus.ACTIVE);
        if (account.getSourceAccountNumber() != null && account.getInitialAmount() != null) {
            Account source = accountRepository.findByAccountNumber(account.getSourceAccountNumber())
                    .orElseThrow(() -> new ApiBankException("Conto sorgente non trovato.", "ACCOUNT_NOT_FOUND"));
            source.setBalance(source.getBalance().subtract(account.getInitialAmount()));
            accountRepository.save(source);
            account.setBalance(account.getInitialAmount());
            log.info("Transferred {} from {} to {}", account.getInitialAmount(), source.getAccountNumber(), account.getAccountNumber());
        }
        account.setSourceAccountNumber(null);
        account.setInitialAmount(null);
        accountRepository.save(account);
        String holderName = account.getUser().getFirstName() + " " + account.getUser().getLastName();
        cardService.issueDebitCard(account.getId(), holderName, "ACTIVE");
        cardService.activateCardsByAccountId(account.getId());
        auditLogService.log("ACCOUNT", account.getId(), "ACTIVATE", "system",
                "Conto attivato: " + accountNumber);
        notificationService.send(account.getUser().getId(), "ACCOUNT", "Il tuo conto " + accountNumber + " è stato attivato.", "NOTIF_ACCOUNT_ACTIVATED", "[\"" + accountNumber + "\"]");
        log.info("Account {} activated by employee, card issued and activated", accountNumber);
    }

    @Transactional
    public void rejectAccountRequest(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.INACTIVE) {
            throw new ApiBankException("Il conto " + accountNumber + " non è in attesa di attivazione.", "INVALID_ACCOUNT_STATE");
        }
        boolean userHasActiveAccount = accountRepository.findByUserId(account.getUser().getId()).stream()
                .anyMatch(a -> a.getStatusId() == AccountStatus.ACTIVE);
        if (!userHasActiveAccount) {
            throw new ApiBankException("Il conto " + accountNumber + " appartiene a una registrazione in corso. Impossibile rifiutare.", "REGISTRATION_ACCOUNT");
        }
        account.setStatusId(AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        account.setSourceAccountNumber(null);
        account.setInitialAmount(null);
        accountRepository.save(account);
        cardService.deleteCardsByAccountId(account.getId());
        auditLogService.log("ACCOUNT", account.getId(), "REJECT", "system",
                "Richiesta conto rifiutata: " + accountNumber);
        notificationService.send(account.getUser().getId(), "ACCOUNT", "La richiesta di apertura conto " + accountNumber + " è stata rifiutata.", "NOTIF_ACCOUNT_REJECTED", "[\"" + accountNumber + "\"]");
        log.info("Account {} request rejected by employee — CLOSED, cards deleted", accountNumber);
    }

    @Transactional(readOnly = true)
    public boolean isLastActiveAccount(Long userId) {
        long activeCount = accountRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatusId() == AccountStatus.ACTIVE)
                .count();
        return activeCount <= 1;
    }

    @Transactional
    public void requestClosure(Long userId, String accountNumber) {
        User user = getUserOrThrow(userId);
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(account, user);

        if (account.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("Solo un conto attivo può essere messo in richiesta di chiusura.", "INVALID_ACCOUNT_STATE");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new ApiBankException("Il conto deve avere fondi zero prima di richiedere la chiusura.", "NON_ZERO_BALANCE");
        }
        if (account.getSourceAccountNumber() != null) {
            throw new ApiBankException("Non è possibile chiudere un conto che ha un trasferimento in sospeso verso un altro conto.", "PENDING_TRANSFER");
        }
        account.setStatusId(AccountStatus.FROZEN);
        account.setClosureRequestedAt(LocalDateTime.now());
        accountRepository.save(account);
        notificationService.send(userId, "ACCOUNT", "Richiesta di chiusura conto " + accountNumber + " inviata. In attesa di approvazione.", "NOTIF_CLOSURE_REQUESTED", "[\"" + accountNumber + "\"]");
        log.info("Closure requested by user id={} for account {}", user.getId(), accountNumber);
    }

    @Transactional
    public void rejectClosure(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.FROZEN) {
            throw new ApiBankException("Il conto " + accountNumber + " non ha richieste di chiusura in sospeso.", "INVALID_ACCOUNT_STATE");
        }
        account.setStatusId(AccountStatus.ACTIVE);
        account.setClosureRequestedAt(null);
        account.setClosureRejectedAt(LocalDateTime.now());
        accountRepository.save(account);
        notificationService.send(account.getUser().getId(), "ACCOUNT", "La richiesta di chiusura del conto " + accountNumber + " è stata rifiutata.", "NOTIF_CLOSURE_REJECTED", "[\"" + accountNumber + "\"]");
        log.info("Closure request rejected for account {} by employee — back to ACTIVE", accountNumber);
    }

    @Transactional
    public void freezeAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("Solo i conti attivi possono essere congelati.", "INVALID_ACCOUNT_STATE");
        }
        account.setStatusId(AccountStatus.FROZEN);
        accountRepository.save(account);
        auditLogService.log("ACCOUNT", account.getId(), "FREEZE", "system",
                "Conto congelato: " + accountNumber);
        notificationService.send(account.getUser().getId(), "ACCOUNT", "Il tuo conto " + accountNumber + " è stato congelato da un impiegato.", "NOTIF_ACCOUNT_FROZEN", "[\"" + accountNumber + "\"]");
        log.info("Account {} frozen by employee", accountNumber);
    }

    @Transactional
    public void unfreezeAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.FROZEN) {
            throw new ApiBankException("Il conto " + accountNumber + " non è congelato.", "INVALID_ACCOUNT_STATE");
        }
        boolean hadClosureRequest = account.getClosureRequestedAt() != null;
        account.setStatusId(AccountStatus.ACTIVE);
        account.setClosureRequestedAt(null);
        if (hadClosureRequest) {
            account.setClosureRejectedAt(LocalDateTime.now());
        }
        accountRepository.save(account);
        auditLogService.log("ACCOUNT", account.getId(), "UNFREEZE", "system",
                "Conto scongelato: " + accountNumber);
        if (hadClosureRequest) {
            notificationService.send(account.getUser().getId(), "ACCOUNT",
                    "La richiesta di chiusura del conto " + accountNumber + " è stata rifiutata. Il conto è stato riattivato.", "NOTIF_CLOSURE_REJECTED_UNFREEZE", "[\"" + accountNumber + "\"]");
        } else {
            notificationService.send(account.getUser().getId(), "ACCOUNT",
                    "Il tuo conto " + accountNumber + " è stato sbloccato.", "NOTIF_ACCOUNT_UNFROZEN", "[\"" + accountNumber + "\"]");
        }
        log.info("Account {} unfrozen by employee", accountNumber);
    }

    @Transactional
    public void validateClosure(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.FROZEN) {
            throw new ApiBankException("Il conto " + accountNumber + " non ha richieste di chiusura in sospeso.", "INVALID_ACCOUNT_STATE");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ApiBankException("Impossibile chiudere il conto " + accountNumber + ": il saldo deve essere zero.", "NON_ZERO_BALANCE");
        }
        account.setStatusId(AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        account.setClosureRequestedAt(null);
        accountRepository.save(account);
        notificationService.send(account.getUser().getId(), "ACCOUNT", "Il tuo conto " + accountNumber + " è stato chiuso.", "NOTIF_ACCOUNT_CLOSED", "[\"" + accountNumber + "\"]");
        log.info("Account {} closed by employee", accountNumber);
    }

    @Transactional
    public Account openAdditionalAccount(Long userId, OpenAccountRequestDto request) {
        User user = getUserOrThrow(userId);

        long activeAccountCount = accountRepository.findByUserId(user.getId()).stream()
                .filter(a -> a.getStatusId() != AccountStatus.CLOSED)
                .count();
        if (activeAccountCount >= MAX_ACCOUNTS_PER_USER) {
            throw new ApiBankException(
                    "Hai raggiunto il numero massimo di conti (" + MAX_ACCOUNTS_PER_USER + ").", "MAX_ACCOUNTS_REACHED");
        }

        boolean hasPendingAccount = accountRepository.findByUserId(user.getId()).stream()
                .anyMatch(a -> a.getStatusId() == AccountStatus.INACTIVE);
        if (hasPendingAccount) {
            throw new ApiBankException(
                    "Hai già un conto in attesa di approvazione. Può richiedere un nuovo conto solo dopo che quello corrente viene accettato.", "PENDING_ACCOUNT_EXISTS");
        }

        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new ApiBankException("Conto sorgente non trovato.", "ACCOUNT_NOT_FOUND"));
        assertOwnership(sourceAccount, user);

        if (sourceAccount.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("Il conto sorgente " + sourceAccount.getAccountNumber() + " non è attivo.", "INVALID_ACCOUNT_STATE");
        }

        if (request.getInitialAmount() == null || request.getInitialAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiBankException("L'importo iniziale deve essere maggiore di 0.", "INVALID_AMOUNT");
        }

        if (sourceAccount.getBalance().compareTo(request.getInitialAmount()) < 0) {
            throw new ApiBankException("Fondi insufficienti sul conto sorgente " + sourceAccount.getAccountNumber() + ".", "INSUFFICIENT_FUNDS");
        }

        Account newAccount = new Account();
        newAccount.setAccountNumber(generateUniqueAccountNumber());
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setStatusId(AccountStatus.INACTIVE);
        newAccount.setUser(user);
        newAccount.setSourceAccountNumber(request.getSourceAccountNumber());
        newAccount.setInitialAmount(request.getInitialAmount());

        newAccount = accountRepository.save(newAccount);

        notificationService.send(userId, "ACCOUNT", "Richiesta di apertura conto " + newAccount.getAccountNumber() + " inviata. In attesa di approvazione.", "NOTIF_ACCOUNT_OPEN_REQUESTED", "[\"" + newAccount.getAccountNumber() + "\"]");

        log.info("User id={} opened additional account {} (INACTIVE), source={}, amount={}", user.getId(), newAccount.getAccountNumber(), request.getSourceAccountNumber(), request.getInitialAmount());
        return newAccount;
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAccountsByUserId(Long userId) {
        User user = getUserOrThrow(userId);
        return accountRepository.findByUserId(user.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonthlySummaryDto getMonthlySummary(Long userId, String accountNumber) {
        User user = getUserOrThrow(userId);
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(account, user);

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        LocalDateTime start = startOfMonth.atStartOfDay();
        LocalDateTime end = endOfMonth.atTime(23, 59, 59);

        Long accountId = account.getId();
        BigDecimal expenses = transactionRepository.sumOutflowByAccountBetween(accountId, start, end);
        BigDecimal income = transactionRepository.sumInflowByAccountBetween(accountId, start, end);
        Long movementCount = transactionRepository.countByAccountBetween(accountId, start, end);

        BigDecimal previousBalance = account.getBalance().subtract(income).add(expenses);
        BigDecimal changePercentage = BigDecimal.ZERO;
        if (previousBalance.compareTo(BigDecimal.ZERO) != 0) {
            changePercentage = account.getBalance()
                    .subtract(previousBalance)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousBalance.abs(), 1, RoundingMode.HALF_UP);
        }

        return MonthlySummaryDto.builder()
                .monthlyExpenses(expenses)
                .monthlyIncome(income)
                .movementCount(movementCount)
                .balanceChangePercentage(changePercentage)
                .build();
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getAccountDetail(Long userId, String accountNumber) {
        User user = getUserOrThrow(userId);
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(account, user);
        return toDto(account);
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getAccountDetailAsEmployee(String accountNumber) {
        return toDto(getAccountOrThrow(accountNumber));
    }

    @Transactional(readOnly = true)
    public AccountHolderDto getAccountHolderInfo(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        User user = account.getUser();
        return AccountHolderDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .accountNumber(account.getAccountNumber())
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeUserDetailDto getEmployeeUserDetail(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        User user = account.getUser();

        var cards = cardRepository.findByAccountId(account.getId()).stream()
                .map(card -> EmployeeUserDetailDto.CardSummaryDto.builder()
                        .id(card.getId())
                        .maskedCardNumber("****" + card.getCardNumber().substring(12))
                        .fullCardNumber(card.getCardNumber())
                        .cvv(card.getCvv())
                        .holderName(card.getHolderName())
                        .expirationDate(card.getExpirationDate())
                        .cardType(card.getCardType().getTypeName())
                        .cardStatus(card.getStatus().getStatusName())
                        .build())
                .toList();

        return EmployeeUserDetailDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .profession(user.getProfession())
                .gender(user.getGender())
                .fiscalCode(user.getFiscalCode())
                .phone(user.getPhone())
                .residence(user.getResidence())
                .birthPlace(user.getBirthPlace())
                .birthProvince(user.getBirthProvince())
                .userStatus(user.getStatus().getUserStatus())
                .userCreatedAt(user.getCreatedAt())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .accountStatus(getStatusName(account.getStatusId()))
                .accountCreatedAt(account.getCreatedAt())
                .closedAt(account.getClosedAt())
                .cards(cards)
                .build();
    }

    @Transactional(readOnly = true)
    public EmployeeUserDetailDto getEmployeeUserDetailByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato con id: " + userId));

        Optional<Account> accountOpt = accountRepository.findByUserId(userId).stream().findFirst();

        EmployeeUserDetailDto.EmployeeUserDetailDtoBuilder builder = EmployeeUserDetailDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .profession(user.getProfession())
                .gender(user.getGender())
                .fiscalCode(user.getFiscalCode())
                .phone(user.getPhone())
                .residence(user.getResidence())
                .birthPlace(user.getBirthPlace())
                .birthProvince(user.getBirthProvince())
                .userStatus(user.getStatus().getUserStatus())
                .userCreatedAt(user.getCreatedAt());

        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            var cards = cardRepository.findByAccountId(account.getId()).stream()
                    .map(card -> EmployeeUserDetailDto.CardSummaryDto.builder()
                            .id(card.getId())
                            .maskedCardNumber("****" + card.getCardNumber().substring(12))
                            .fullCardNumber(card.getCardNumber())
                            .cvv(card.getCvv())
                            .holderName(card.getHolderName())
                            .expirationDate(card.getExpirationDate())
                            .cardType(card.getCardType().getTypeName())
                            .cardStatus(card.getStatus().getStatusName())
                            .build())
                    .toList();

            builder.accountNumber(account.getAccountNumber())
                    .balance(account.getBalance())
                    .accountStatus(getStatusName(account.getStatusId()))
                    .accountCreatedAt(account.getCreatedAt())
                    .closedAt(account.getClosedAt())
                    .cards(cards);
        }

        return builder.build();
    }

    private String getStatusName(Integer statusId) {
        if (statusId == null) return "Sconosciuto";
        return switch (statusId) {
            case 1 -> "INATTIVO";
            case 2 -> "ATTIVO";
            case 3 -> "CONGELATO";
            case 4 -> "CHIUSO";
            default -> "SCONOSCIUTO";
        };
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAllAccountsByStatus(Integer statusId) {
        List<Account> accounts = statusId == null
                ? accountRepository.findAll()
                : accountRepository.findByStatusId(statusId);
        return accounts.stream().map(this::toDto).toList();
    }

    private Account getAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("Conto " + accountNumber + " non trovato.", "ACCOUNT_NOT_FOUND"));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));
    }

    private void assertOwnership(Account account, User user) {
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ApiBankException("Il conto " + account.getAccountNumber() + " non appartiene all'utente corrente.", "FORBIDDEN");
        }
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_IBAN_GENERATION_ATTEMPTS; attempt++) {
            String candidate = "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new ApiBankException("Errore tecnico nella generazione del numero di conto univoco, riprova.", "IBAN_GENERATION_FAILED");
    }

    private AccountResponseDto toDto(Account account) {
        return AccountResponseDto.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .statusId(account.getStatusId())
                .profileId(account.getUser().getId())
                .profileFirstName(account.getUser().getFirstName())
                .profileLastName(account.getUser().getLastName())
                .userStatusId(account.getUser().getStatus().getId())
                .initialAmount(account.getInitialAmount())
                .createdAt(account.getCreatedAt())
                .closedAt(account.getClosedAt())
                .build();
    }
}
