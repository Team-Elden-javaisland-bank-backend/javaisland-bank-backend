package com.javaisland.bank_backend.account;

import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.transaction.TransactionService;
import com.javaisland.bank_backend.transaction.TransactionType;
import com.javaisland.bank_backend.user.User;
import com.javaisland.bank_backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final int MAX_IBAN_GENERATION_ATTEMPTS = 5;

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Account createInitialAccountForUser(User user) {
        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setStatusId(AccountStatus.INACTIVE); // becomes usable only once an employee validates the registration
        account.setUser(user);
        Account saved = accountRepository.save(account);
        log.info("Created initial account {} (INACTIVE) for user id={}", saved.getAccountNumber(), user.getId());
        return saved;
    }

    @Transactional
    public void activateInitialAccountForUser(Long userId) {
        Account account = accountRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatusId() == AccountStatus.INACTIVE)
                .findFirst()
                .orElseThrow(() -> new ApiBankException(
                        "No pending account found for this registration.", "ACCOUNT_NOT_FOUND"));
        account.setStatusId(AccountStatus.ACTIVE);
        accountRepository.save(account);
        log.info("Account {} activated as part of registration validation for user id={}", account.getAccountNumber(), userId);
    }

    @Transactional
    public void activateAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.INACTIVE) {
            throw new ApiBankException("Account " + accountNumber + " is not pending activation.", "INVALID_ACCOUNT_STATE");
        }
        account.setStatusId(AccountStatus.ACTIVE);
        accountRepository.save(account);
        log.info("Account {} activated by employee", accountNumber);
    }

    @Transactional
    public void requestClosure(String keycloakId, String accountNumber) {
        User user = getUserOrThrow(keycloakId);
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(account, user);

        if (account.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("Only an active account can be put in closure request state.", "INVALID_ACCOUNT_STATE");
        }
        account.setStatusId(AccountStatus.FROZEN);
        accountRepository.save(account);
        log.info("Closure requested by user id={} for account {}", user.getId(), accountNumber);
    }

    @Transactional
    public void validateClosure(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.FROZEN) {
            throw new ApiBankException("Account " + accountNumber + " has no pending closure request.", "INVALID_ACCOUNT_STATE");
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new ApiBankException("Cannot close account " + accountNumber + ": balance must be zero.", "NON_ZERO_BALANCE");
        }
        account.setStatusId(AccountStatus.CLOSED);
        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);
        log.info("Account {} closed by employee", accountNumber);
    }

    @Transactional
    public Account openAdditionalAccount(String keycloakId, OpenAccountRequestDto request) {
        User user = getUserOrThrow(keycloakId);

        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new ApiBankException("Source account not found.", "ACCOUNT_NOT_FOUND"));
        assertOwnership(sourceAccount, user);

        if (sourceAccount.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("Source account " + sourceAccount.getAccountNumber() + " is not active.", "INVALID_ACCOUNT_STATE");
        }

        Account newAccount = new Account();
        newAccount.setAccountNumber(generateUniqueAccountNumber());
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setStatusId(AccountStatus.ACTIVE); // internally funded, no employee validation needed
        newAccount.setUser(user);
        newAccount = accountRepository.save(newAccount);

        transactionService.transferFunds(
                sourceAccount,
                newAccount,
                request.getInitialAmount(),
                TransactionType.INITIAL_TRANSFER,
                "Opening additional account " + newAccount.getAccountNumber()
        );

        log.info("User id={} opened additional account {} funded from {}", user.getId(), newAccount.getAccountNumber(), sourceAccount.getAccountNumber());
        return newAccount;
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getAccountDetail(String keycloakId, String accountNumber) {
        User user = getUserOrThrow(keycloakId);
        Account account = getAccountOrThrow(accountNumber);
        assertOwnership(account, user);
        return toDto(account);
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getBalance(String keycloakId, String accountNumber) {
        return getAccountDetail(keycloakId, accountNumber);
    }

    @Transactional(readOnly = true)
    public AccountResponseDto getAccountDetailAsEmployee(String accountNumber) {
        return toDto(getAccountOrThrow(accountNumber));
    }

    private Account getAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("Account " + accountNumber + " not found.", "ACCOUNT_NOT_FOUND"));
    }

    private User getUserOrThrow(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ApiBankException("User not found.", "USER_NOT_FOUND"));
    }

    private void assertOwnership(Account account, User user) {
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ApiBankException("Account " + account.getAccountNumber() + " does not belong to the current user.", "FORBIDDEN");
        }
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 0; attempt < MAX_IBAN_GENERATION_ATTEMPTS; attempt++) {
            String candidate = "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new ApiBankException("Technical error generating a unique account number, please retry.", "IBAN_GENERATION_FAILED");
    }

    private AccountResponseDto toDto(Account account) {
        return AccountResponseDto.builder()
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .statusId(account.getStatusId())
                .createdAt(account.getCreatedAt())
                .closedAt(account.getClosedAt())
                .build();
    }
}
