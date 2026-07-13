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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountLimitService {

    private static final String TYPE_SINGLE = "SINGLE_TRANSFER";
    private static final String TYPE_DAILY = "DAILY_TRANSFER";
    private static final String TYPE_MONTHLY = "MONTHLY_TRANSFER";

    private final AccountLimitRepository accountLimitRepository;
    private final AccountRepository accountRepository;
    private final LimitTypeRepository limitTypeRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public AccountLimitResponseDto setLimit(String accountNumber, String limitTypeName, SetLimitRequestDto request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("Account " + accountNumber + " not found.", "ACCOUNT_NOT_FOUND"));

        LimitType limitType = limitTypeRepository.findByLimitName(limitTypeName)
                .orElseThrow(() -> new ApiBankException("Limit type '" + limitTypeName + "' not found.", "LIMIT_TYPE_NOT_FOUND"));

        AccountLimit limit = accountLimitRepository.findByAccountAndLimitType(account, limitType)
                .orElseGet(AccountLimit::new);

        limit.setAccount(account);
        limit.setLimitType(limitType);
        limit.setMaxAmount(request.getMaxAmount());

        return mapToDto(accountLimitRepository.save(limit));
    }

    @Transactional(readOnly = true)
    public List<AccountLimitResponseDto> getLimits(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiBankException("Account " + accountNumber + " not found.", "ACCOUNT_NOT_FOUND"));

        return accountLimitRepository.findByAccountId(account.getId())
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void validateTransfer(Account sourceAccount, BigDecimal amount) {
        List<AccountLimit> limits = accountLimitRepository.findByAccountId(sourceAccount.getId());

        for (AccountLimit limit : limits) {
            String typeName = limit.getLimitType().getLimitName();
            BigDecimal max = limit.getMaxAmount();

            switch (typeName) {
                case TYPE_SINGLE -> {
                    if (amount.compareTo(max) > 0) {
                        throw new ApiBankException(
                            "Transfer amount exceeds single transfer limit of " + max + ".", "LIMIT_EXCEEDED");
                    }
                }
                case TYPE_DAILY -> {
                    LocalDateTime dayStart = LocalDate.now().atStartOfDay();
                    LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX);
                    BigDecimal sumToday = transactionRepository.sumAmountBySourceAccountAndTypeAndStatusBetween(
                            sourceAccount.getId(), TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                            dayStart, dayEnd);
                    if (sumToday.add(amount).compareTo(max) > 0) {
                        throw new ApiBankException(
                            "Daily transfer limit of " + max + " would be exceeded. Already used: " + sumToday + ".",
                            "LIMIT_EXCEEDED");
                    }
                }
                case TYPE_MONTHLY -> {
                    LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                    LocalDateTime monthEnd = LocalDate.now().atTime(LocalTime.MAX);
                    BigDecimal sumMonth = transactionRepository.sumAmountBySourceAccountAndTypeAndStatusBetween(
                            sourceAccount.getId(), TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                            monthStart, monthEnd);
                    if (sumMonth.add(amount).compareTo(max) > 0) {
                        throw new ApiBankException(
                            "Monthly transfer limit of " + max + " would be exceeded. Already used: " + sumMonth + ".",
                            "LIMIT_EXCEEDED");
                    }
                }
            }
        }
    }

    private AccountLimitResponseDto mapToDto(AccountLimit limit) {
        return AccountLimitResponseDto.builder()
                .id(limit.getId())
                .limitType(limit.getLimitType().getLimitName())
                .maxAmount(limit.getMaxAmount())
                .updatedAt(limit.getUpdatedAt())
                .build();
    }
}
