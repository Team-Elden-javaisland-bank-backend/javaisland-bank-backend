package com.javaisland.bank_backend.account.service;

import com.javaisland.bank_backend.account.dto.OpenAccountRequestDto;
import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.audit.service.AuditLogService;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountManagementService {

    private static final int MAX_IBAN_GENERATION_ATTEMPTS = 5;
    private static final int MAX_ACCOUNTS_PER_USER = 3;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CardService cardService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final IbanGenerator ibanGenerator;

    @Transactional(propagation = Propagation.REQUIRED)
    public Account createInitialAccountForUser(User user) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));
        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setStatusId(AccountStatus.INACTIVE);
        account.setIsLimitsConfigured(false);
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
            throw new ApiBankException("INVALID_STATE", "INVALID_STATE");
        }
        Account account = accountRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatusId() == AccountStatus.INACTIVE)
                .findFirst()
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));
        account.setStatusId(AccountStatus.ACTIVE);
        accountRepository.save(account);
        log.info("Account {} activated as part of registration validation for user id={}", account.getAccountNumber(), userId);
    }

    @Transactional
    public void activateAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.INACTIVE) {
            throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
        }
        boolean userHasActiveAccount = accountRepository.findByUserId(account.getUser().getId()).stream()
                .anyMatch(a -> a.getStatusId() == AccountStatus.ACTIVE);
        if (!userHasActiveAccount) {
            throw new ApiBankException("REGISTRATION_ACCOUNT", "REGISTRATION_ACCOUNT");
        }
        account.setStatusId(AccountStatus.ACTIVE);
        if (account.getSourceAccountNumber() != null && account.getInitialAmount() != null) {
            Account source = accountRepository.findByAccountNumberForUpdate(account.getSourceAccountNumber())
                    .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));
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
                "ACCOUNT_ACTIVATED: " + accountNumber);
        notificationService.send(account.getUser().getId(), "ACCOUNT", "Your account " + accountNumber + " has been activated.", "NOTIF_ACCOUNT_ACTIVATED", "[\"" + accountNumber + "\"]");
        log.info("Account {} activated by employee, card issued and activated", accountNumber);
    }

    @Transactional
    public void rejectAccountRequest(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.INACTIVE) {
            throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
        }
        boolean userHasActiveAccount = accountRepository.findByUserId(account.getUser().getId()).stream()
                .anyMatch(a -> a.getStatusId() == AccountStatus.ACTIVE);
        if (!userHasActiveAccount) {
            throw new ApiBankException("REGISTRATION_ACCOUNT", "REGISTRATION_ACCOUNT");
        }
        account.setStatusId(AccountStatus.CLOSED);
        account.setClosedAt(OffsetDateTime.now());
        account.setSourceAccountNumber(null);
        account.setInitialAmount(null);
        accountRepository.save(account);
        cardService.deleteCardsByAccountId(account.getId());
        auditLogService.log("ACCOUNT", account.getId(), "REJECT", "system",
                "ACCOUNT_REJECTED: " + accountNumber);
        notificationService.send(account.getUser().getId(), "ACCOUNT", "Account opening request " + accountNumber + " has been rejected.", "NOTIF_ACCOUNT_REJECTED", "[\"" + accountNumber + "\"]");
        log.info("Account {} request rejected by employee — CLOSED, cards deleted", accountNumber);
    }

    @Transactional
    public void requestClosure(Long userId, String accountNumber) {
        User user = getUserOrThrow(userId);
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(account, user);

        long activeCount = accountRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatusId() == AccountStatus.ACTIVE)
                .count();
        if (activeCount <= 1) {
            throw new ApiBankException("LAST_ACTIVE_ACCOUNT", "LAST_ACTIVE_ACCOUNT");
        }

        if (account.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new ApiBankException("NON_ZERO_BALANCE", "NON_ZERO_BALANCE");
        }
        if (account.getSourceAccountNumber() != null) {
            throw new ApiBankException("PENDING_TRANSFER", "PENDING_TRANSFER");
        }
        account.setStatusId(AccountStatus.FROZEN);
        account.setClosureRequestedAt(OffsetDateTime.now());
        accountRepository.save(account);
        cardService.blockCardsByAccountId(account.getId());
        notificationService.send(userId, "ACCOUNT", "Closure request for account " + accountNumber + " submitted. Awaiting approval.", "NOTIF_CLOSURE_REQUESTED", "[\"" + accountNumber + "\"]");
        log.info("Closure requested by user id={} for account {} — cards blocked", user.getId(), accountNumber);
    }

    @Transactional
    public void rejectClosure(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.FROZEN) {
            throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
        }
        account.setStatusId(AccountStatus.ACTIVE);
        account.setClosureRequestedAt(null);
        account.setClosureRejectedAt(OffsetDateTime.now());
        accountRepository.save(account);
        cardService.unblockCardsByAccountId(account.getId());
        notificationService.send(account.getUser().getId(), "ACCOUNT", "Closure request for account " + accountNumber + " has been rejected.", "NOTIF_CLOSURE_REJECTED", "[\"" + accountNumber + "\"]");
        log.info("Closure request rejected for account {} by employee — back to ACTIVE, cards unblocked", accountNumber);
    }

    @Transactional
    public void freezeAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
        }
        account.setStatusId(AccountStatus.FROZEN);
        accountRepository.save(account);
        cardService.blockCardsByAccountId(account.getId());
        auditLogService.log("ACCOUNT", account.getId(), "FREEZE", "system",
                "ACCOUNT_FROZEN: " + accountNumber);
        notificationService.send(account.getUser().getId(), "ACCOUNT", "Your account " + accountNumber + " has been frozen by an employee.", "NOTIF_ACCOUNT_FROZEN", "[\"" + accountNumber + "\"]");
        log.info("Account {} frozen by employee — cards blocked", accountNumber);
    }

    @Transactional
    public void unfreezeAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.FROZEN) {
            throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
        }
        boolean hadClosureRequest = account.getClosureRequestedAt() != null;
        account.setStatusId(AccountStatus.ACTIVE);
        account.setClosureRequestedAt(null);
        if (hadClosureRequest) {
            account.setClosureRejectedAt(OffsetDateTime.now());
        }
        accountRepository.save(account);
        cardService.unblockCardsByAccountId(account.getId());
        auditLogService.log("ACCOUNT", account.getId(), "UNFREEZE", "system",
                "ACCOUNT_UNFROZEN: " + accountNumber);
        if (hadClosureRequest) {
            notificationService.send(account.getUser().getId(), "ACCOUNT",
                    "Closure request for account " + accountNumber + " has been rejected. Account reactivated.", "NOTIF_CLOSURE_REJECTED_UNFREEZE", "[\"" + accountNumber + "\"]");
        } else {
            notificationService.send(account.getUser().getId(), "ACCOUNT",
                    "Your account " + accountNumber + " has been unfrozen.", "NOTIF_ACCOUNT_UNFROZEN", "[\"" + accountNumber + "\"]");
        }
        log.info("Account {} unfrozen by employee", accountNumber);
    }

    @Transactional
    public void validateClosure(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.FROZEN) {
            throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ApiBankException("NON_ZERO_BALANCE", "NON_ZERO_BALANCE");
        }
        account.setStatusId(AccountStatus.CLOSED);
        account.setClosedAt(OffsetDateTime.now());
        account.setClosureRequestedAt(null);
        accountRepository.save(account);
        cardService.closeCardsByAccountId(account.getId());
        notificationService.send(account.getUser().getId(), "ACCOUNT", "Your account " + accountNumber + " has been closed.", "NOTIF_ACCOUNT_CLOSED", "[\"" + accountNumber + "\"]");
        log.info("Account {} closed by employee — cards closed", accountNumber);
    }

    @Transactional
    public Account openAdditionalAccount(Long userId, OpenAccountRequestDto request) {
        User user = getUserOrThrow(userId);

        long activeAccountCount = accountRepository.findByUserId(user.getId()).stream()
                .filter(a -> a.getStatusId() != AccountStatus.CLOSED)
                .count();
        if (activeAccountCount >= MAX_ACCOUNTS_PER_USER) {
            throw new ApiBankException("MAX_ACCOUNTS_REACHED", "MAX_ACCOUNTS_REACHED");
        }

        boolean hasPendingAccount = accountRepository.findByUserId(user.getId()).stream()
                .anyMatch(a -> a.getStatusId() == AccountStatus.INACTIVE);
        if (hasPendingAccount) {
            throw new ApiBankException("PENDING_ACCOUNT_EXISTS", "PENDING_ACCOUNT_EXISTS");
        }

        Account sourceAccount = accountRepository.findByAccountNumberForUpdate(request.getSourceAccountNumber())
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));
        assertOwnership(sourceAccount, user);

        if (sourceAccount.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
        }

        if (request.getInitialAmount() == null || request.getInitialAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiBankException("INVALID_AMOUNT", "INVALID_AMOUNT");
        }

        if (sourceAccount.getBalance().compareTo(request.getInitialAmount()) < 0) {
            throw new ApiBankException("INSUFFICIENT_FUNDS", "INSUFFICIENT_FUNDS");
        }

        Account newAccount = new Account();
        newAccount.setAccountNumber(generateUniqueAccountNumber());
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setStatusId(AccountStatus.INACTIVE);
        newAccount.setIsLimitsConfigured(false);
        newAccount.setUser(user);
        newAccount.setSourceAccountNumber(request.getSourceAccountNumber());
        newAccount.setInitialAmount(request.getInitialAmount());

        newAccount = accountRepository.save(newAccount);

        notificationService.send(userId, "ACCOUNT", "Account opening request " + newAccount.getAccountNumber() + " submitted. Awaiting approval.", "NOTIF_ACCOUNT_OPEN_REQUESTED", "[\"" + newAccount.getAccountNumber() + "\"]");

        log.info("User id={} opened additional account {} (INACTIVE), source={}, amount={}", user.getId(), newAccount.getAccountNumber(), request.getSourceAccountNumber(), request.getInitialAmount());
        return newAccount;
    }

    @Transactional
    public void markLimitsConfigured(Long userId, String accountNumber) {
        User user = getUserOrThrow(userId);
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(account, user);
        if (account.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
        }
        account.setIsLimitsConfigured(true);
        accountRepository.save(account);
        log.info("Limits configured for account {} (user id={})", accountNumber, userId);
    }

    @Transactional
    public void completeLimitsSetup(Long userId) {
        User user = getUserOrThrow(userId);
        user.setLimitsSetupComplete(true);
        userRepository.save(user);
        accountRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatusId() != AccountStatus.CLOSED)
                .forEach(a -> {
                    a.setIsLimitsConfigured(true);
                    accountRepository.save(a);
                });
        log.info("Initial limits setup completed for user id={}", userId);
    }

    private Account getAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));
    }

    private void assertOwnership(Account account, User user) {
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ApiBankException("FORBIDDEN", "FORBIDDEN");
        }
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_IBAN_GENERATION_ATTEMPTS; attempt++) {
            String candidate = ibanGenerator.generate();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new ApiBankException("IBAN_GENERATION_FAILED", "IBAN_GENERATION_FAILED");
    }
}
