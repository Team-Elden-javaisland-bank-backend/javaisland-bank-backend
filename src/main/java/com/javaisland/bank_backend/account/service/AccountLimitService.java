package com.javaisland.bank_backend.account.service;

import com.javaisland.bank_backend.account.dto.AccountLimitResponseDto;
import com.javaisland.bank_backend.account.dto.SetLimitRequestDto;
import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountLimit;
import com.javaisland.bank_backend.account.model.LimitType;
import com.javaisland.bank_backend.account.repository.AccountLimitRepository;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.repository.LimitTypeRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.transaction.model.TransactionStatus;
import com.javaisland.bank_backend.transaction.model.TransactionType;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountLimitService {

    private static final String TYPE_SINGLE = "SINGLE_TRANSFER";
    private static final String TYPE_DAILY = "DAILY_TRANSFER";
    private static final String TYPE_MONTHLY = "MONTHLY_TRANSFER";

    private static final Map<String, BigDecimal> MAX_VALUES = Map.of(
            "ATM_WITHDRAWAL", new BigDecimal("300"),
            "POS_SPENDING", new BigDecimal("2500"),
            "SINGLE_TRANSFER", new BigDecimal("10000"),
            "INSTANT_TRANSFER_SINGLE", new BigDecimal("5000"),
            "DAILY_TRANSFER", new BigDecimal("15000"),
            "MONTHLY_TRANSFER", new BigDecimal("50000")
    );

    private static final Map<String, BigDecimal> MIN_VALUES = Map.of(
            "ATM_WITHDRAWAL", new BigDecimal("10"),
            "POS_SPENDING", new BigDecimal("0.10"),
            "SINGLE_TRANSFER", new BigDecimal("1"),
            "INSTANT_TRANSFER_SINGLE", new BigDecimal("1"),
            "DAILY_TRANSFER", new BigDecimal("1"),
            "MONTHLY_TRANSFER", new BigDecimal("1")
    );

    private final AccountLimitRepository accountLimitRepository;
    private final AccountRepository accountRepository;
    private final LimitTypeRepository limitTypeRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public AccountLimitResponseDto setLimit(String accountNumber, String limitTypeName, SetLimitRequestDto request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));

        LimitType limitType = limitTypeRepository.findByLimitName(limitTypeName)
                .orElseThrow(() -> new ApiBankException("LIMIT_TYPE_NOT_FOUND", "LIMIT_TYPE_NOT_FOUND"));

        BigDecimal maxAllowed = MAX_VALUES.get(limitTypeName);
        if (maxAllowed != null && request.getMaxAmount().compareTo(maxAllowed) > 0) {
            throw new ApiBankException("LIMIT_EXCEEDS_MAX", "LIMIT_EXCEEDS_MAX");
        }

        BigDecimal minAllowed = MIN_VALUES.getOrDefault(limitTypeName, BigDecimal.ZERO);
        if (request.getMaxAmount().compareTo(minAllowed) < 0) {
            throw new ApiBankException("LIMIT_BELOW_MIN", "LIMIT_BELOW_MIN");
        }

        AccountLimit limit = accountLimitRepository.findByAccountAndLimitType(account, limitType)
                .orElseGet(AccountLimit::new);

        limit.setAccount(account);
        limit.setLimitType(limitType);
        limit.setMaxAmount(request.getMaxAmount());

        return mapToDto(accountLimitRepository.save(limit));
    }

    @Transactional
    public AccountLimitResponseDto setLimitAsCustomer(Long userId, String accountNumber, String limitTypeName, SetLimitRequestDto request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));

        if (!account.getUser().getId().equals(userId)) {
            throw new ApiBankException("FORBIDDEN", "FORBIDDEN");
        }

        LimitType limitType = limitTypeRepository.findByLimitName(limitTypeName)
                .orElseThrow(() -> new ApiBankException("LIMIT_TYPE_NOT_FOUND", "LIMIT_TYPE_NOT_FOUND"));

        boolean isInitialSetup = !Boolean.TRUE.equals(account.getIsLimitsConfigured())
                || !Boolean.TRUE.equals(account.getUser().isLimitsSetupComplete());

        if (limitType.getChangePolicy() == LimitType.ChangePolicy.BANK_ONLY) {
            AccountLimit existingCheck = accountLimitRepository.findByAccountAndLimitType(account, limitType).orElse(null);
            if (existingCheck != null && !isInitialSetup) {
                throw new ApiBankException("BANK_ONLY_LIMIT", "BANK_ONLY_LIMIT");
            }
        }

        BigDecimal maxAllowed = MAX_VALUES.get(limitTypeName);
        if (maxAllowed != null && request.getMaxAmount().compareTo(maxAllowed) > 0) {
            throw new ApiBankException("LIMIT_EXCEEDS_MAX", "LIMIT_EXCEEDS_MAX");
        }

        BigDecimal minAllowed = MIN_VALUES.getOrDefault(limitTypeName, BigDecimal.ZERO);
        if (request.getMaxAmount().compareTo(minAllowed) < 0) {
            throw new ApiBankException("LIMIT_BELOW_MIN", "LIMIT_BELOW_MIN");
        }

        AccountLimit existing = accountLimitRepository.findByAccountAndLimitType(account, limitType).orElse(null);

        if (limitType.getChangePolicy() == LimitType.ChangePolicy.USER_LOWER_ONLY && existing != null && !isInitialSetup) {
            if (request.getMaxAmount().compareTo(existing.getMaxAmount()) > 0) {
                throw new ApiBankException("LOWER_ONLY_LIMIT", "LOWER_ONLY_LIMIT");
            }
        }

        AccountLimit limit = existing != null ? existing : new AccountLimit();
        limit.setAccount(account);
        limit.setLimitType(limitType);
        limit.setMaxAmount(request.getMaxAmount());

        return mapToDto(accountLimitRepository.save(limit));
    }

    @Transactional(readOnly = true)
    public List<AccountLimitResponseDto> getLimits(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));

        return accountLimitRepository.findByAccountId(account.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void validateTransfer(Account sourceAccount, BigDecimal amount, boolean isInstant) {
        List<AccountLimit> limits = accountLimitRepository.findByAccountId(sourceAccount.getId());

        for (AccountLimit limit : limits) {
            String typeName = limit.getLimitType().getLimitName();
            BigDecimal max = limit.getMaxAmount();

            switch (typeName) {
                case TYPE_SINGLE -> {
                    if (!isInstant && amount.compareTo(max) > 0) {
                        throw new ApiBankException(
                            "LIMIT_EXCEEDED", "LIMIT_EXCEEDED");
                    }
                }
                case TYPE_DAILY -> {
                    OffsetDateTime dayStart = LocalDate.now().atStartOfDay(ZoneId.of("Europe/Rome")).toOffsetDateTime();
                    OffsetDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime();
                    BigDecimal sumToday = transactionRepository.sumAmountBySourceAccountAndTypeAndStatusBetween(
                            sourceAccount.getId(), TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                            dayStart, dayEnd);
                    if (sumToday.add(amount).compareTo(max) > 0) {
                        throw new ApiBankException(
                            "LIMIT_EXCEEDED", "LIMIT_EXCEEDED");
                    }
                }
                case TYPE_MONTHLY -> {
                    OffsetDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.of("Europe/Rome")).toOffsetDateTime();
                    OffsetDateTime monthEnd = LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.of("Europe/Rome")).toOffsetDateTime();
                    BigDecimal sumMonth = transactionRepository.sumAmountBySourceAccountAndTypeAndStatusBetween(
                            sourceAccount.getId(), TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                            monthStart, monthEnd);
                    if (sumMonth.add(amount).compareTo(max) > 0) {
                        throw new ApiBankException(
                            "LIMIT_EXCEEDED", "LIMIT_EXCEEDED");
                    }
                }
                case "INSTANT_TRANSFER_SINGLE" -> {
                    if (isInstant && amount.compareTo(max) > 0) {
                        throw new ApiBankException(
                            "LIMIT_EXCEEDED", "LIMIT_EXCEEDED");
                    }
                }
            }
        }
    }

    public void validateWithdrawal(Account sourceAccount, BigDecimal amount) {
        List<AccountLimit> limits = accountLimitRepository.findByAccountId(sourceAccount.getId());

        boolean foundAtmLimit = false;
        for (AccountLimit limit : limits) {
            String typeName = limit.getLimitType().getLimitName();
            BigDecimal max = limit.getMaxAmount();

            if ("ATM_WITHDRAWAL".equals(typeName)) {
                foundAtmLimit = true;
                if (amount.compareTo(max) > 0) {
                    throw new ApiBankException(
                        "LIMIT_EXCEEDED", "LIMIT_EXCEEDED");
                }
            }
        }

        if (!foundAtmLimit) {
            BigDecimal defaultMax = MAX_VALUES.get("ATM_WITHDRAWAL");
            if (defaultMax != null && amount.compareTo(defaultMax) > 0) {
                throw new ApiBankException(
                    "LIMIT_EXCEEDED", "LIMIT_EXCEEDED");
            }
        }
    }

    private AccountLimitResponseDto mapToDto(AccountLimit limit) {
        return AccountLimitResponseDto.builder()
                .id(limit.getId())
                .limitType(limit.getLimitType().getLimitName())
                .maxAmount(limit.getMaxAmount())
                .updatedAt(limit.getUpdatedAt())
                .changePolicy(limit.getLimitType().getChangePolicy().name())
                .build();
    }
}
