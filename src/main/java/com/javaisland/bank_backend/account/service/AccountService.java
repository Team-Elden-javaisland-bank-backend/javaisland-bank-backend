package com.javaisland.bank_backend.account.service;

import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import com.javaisland.bank_backend.account.dto.CloseAccountRequestDto;
import com.javaisland.bank_backend.account.dto.OpenAccountRequestDto;
import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.transaction.service.TransactionService;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ApiBankException("User not found.", "USER_NOT_FOUND"));
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
    public void requestClosure(Long userId, String accountNumber) {
        User user = getUserOrThrow(userId);
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
    public void rejectClosure(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.FROZEN) {
            throw new ApiBankException("Account " + accountNumber + " has no pending closure request.", "INVALID_ACCOUNT_STATE");
        }
        account.setStatusId(AccountStatus.ACTIVE);
        accountRepository.save(account);
        log.info("Closure request rejected for account {} by employee — back to ACTIVE", accountNumber);
    }

    @Transactional
    public void freezeAccount(String accountNumber) {
        Account account = getAccountOrThrow(accountNumber);
        if (account.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("Only active accounts can be frozen.", "INVALID_ACCOUNT_STATE");
        }
        account.setStatusId(AccountStatus.FROZEN);
        accountRepository.save(account);
        log.info("Account {} frozen by employee", accountNumber);
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
    public Account openAdditionalAccount(Long userId, OpenAccountRequestDto request) {
        User user = getUserOrThrow(userId);

        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new ApiBankException("Source account not found.", "ACCOUNT_NOT_FOUND"));
        assertOwnership(sourceAccount, user);

        if (sourceAccount.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("Source account " + sourceAccount.getAccountNumber() + " is not active.", "INVALID_ACCOUNT_STATE");
        }

        Account newAccount = new Account();
        newAccount.setAccountNumber(generateUniqueAccountNumber());
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setStatusId(AccountStatus.ACTIVE);
        newAccount.setUser(user);
        newAccount = accountRepository.save(newAccount);

        transactionService.transferFunds(
                sourceAccount,
                newAccount,
                request.getInitialAmount(),
                "INITIAL_TRANSFER",
                "COMPLETED",
                "Opening additional account " + newAccount.getAccountNumber()
        );

        log.info("User id={} opened additional account {} funded from {}", user.getId(), newAccount.getAccountNumber(), sourceAccount.getAccountNumber());
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
    public List<AccountResponseDto> getAllAccountsByStatus(Integer statusId) {
        List<Account> accounts = statusId == null
                ? accountRepository.findAll()
                : accountRepository.findByStatusId(statusId);
        return accounts.stream().map(this::toDto).toList();
    }

    private Account getAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("Account " + accountNumber + " not found.", "ACCOUNT_NOT_FOUND"));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
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
                .profileId(account.getUser().getId())
                .createdAt(account.getCreatedAt())
                .closedAt(account.getClosedAt())
                .build();
    }
}
