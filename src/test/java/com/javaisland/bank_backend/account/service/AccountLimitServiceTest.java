package com.javaisland.bank_backend.account.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLimitServiceTest {

    @Mock private AccountLimitRepository accountLimitRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private LimitTypeRepository limitTypeRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private AccountLimitService accountLimitService;

    @Test
    void setLimit_createsNewLimit() {
        var account = new Account(); account.setId(1L); account.setAccountNumber("IT123");
        var limitType = new LimitType(1, "DAILY_TRANSFER", "desc", LimitType.ChangePolicy.BANK_ONLY);
        var request = new SetLimitRequestDto(); request.setMaxAmount(new BigDecimal("5000"));

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("DAILY_TRANSFER")).thenReturn(Optional.of(limitType));
        when(accountLimitRepository.findByAccountAndLimitType(account, limitType)).thenReturn(Optional.empty());
        when(accountLimitRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = accountLimitService.setLimit("IT123", "DAILY_TRANSFER", request);

        assertEquals("DAILY_TRANSFER", result.getLimitType());
        assertEquals(0, new BigDecimal("5000").compareTo(result.getMaxAmount()));
    }

    @Test
    void setLimit_updatesExisting() {
        var account = new Account(); account.setId(1L); account.setAccountNumber("IT123");
        var limitType = new LimitType(1, "DAILY_TRANSFER", "desc", LimitType.ChangePolicy.BANK_ONLY);
        var existing = new AccountLimit(); existing.setMaxAmount(new BigDecimal("1000"));
        var request = new SetLimitRequestDto(); request.setMaxAmount(new BigDecimal("5000"));

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("DAILY_TRANSFER")).thenReturn(Optional.of(limitType));
        when(accountLimitRepository.findByAccountAndLimitType(account, limitType)).thenReturn(Optional.of(existing));
        when(accountLimitRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = accountLimitService.setLimit("IT123", "DAILY_TRANSFER", request);

        assertEquals(0, new BigDecimal("5000").compareTo(result.getMaxAmount()));
    }

    @Test
    void setLimit_accountNotFound() {
        when(accountRepository.findByAccountNumber("IT999")).thenReturn(Optional.empty());

        var request = new SetLimitRequestDto(); request.setMaxAmount(new BigDecimal("100"));
        assertThrows(ApiBankException.class, () ->
                accountLimitService.setLimit("IT999", "DAILY_TRANSFER", request));
    }

    @Test
    void setLimit_typeNotFound() {
        var account = new Account(); account.setAccountNumber("IT123");
        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("FAKE_TYPE")).thenReturn(Optional.empty());

        var request = new SetLimitRequestDto(); request.setMaxAmount(new BigDecimal("100"));
        assertThrows(ApiBankException.class, () ->
                accountLimitService.setLimit("IT123", "FAKE_TYPE", request));
    }

    @Test
    void getLimits_returnsList() {
        var account = new Account(); account.setId(1L); account.setAccountNumber("IT123");
        var limit = new AccountLimit();
        limit.setMaxAmount(new BigDecimal("5000"));
        var lt = new LimitType(1, "DAILY_TRANSFER", null, LimitType.ChangePolicy.BANK_ONLY);
        limit.setLimitType(lt);

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(accountLimitRepository.findByAccountId(1L)).thenReturn(List.of(limit));

        var results = accountLimitService.getLimits("IT123");

        assertEquals(1, results.size());
        assertEquals("DAILY_TRANSFER", results.get(0).getLimitType());
    }

    @Test
    void getLimits_accountNotFound() {
        when(accountRepository.findByAccountNumber("IT999")).thenReturn(Optional.empty());
        assertThrows(ApiBankException.class, () -> accountLimitService.getLimits("IT999"));
    }

    @Test
    void validateTransfer_singleLimitExceeded() {
        var account = new Account(); account.setId(1L);
        var limit = new AccountLimit();
        limit.setMaxAmount(new BigDecimal("1000"));
        var lt = new LimitType(1, "SINGLE_TRANSFER", null, LimitType.ChangePolicy.BANK_ONLY);
        limit.setLimitType(lt);

        when(accountLimitRepository.findByAccountId(1L)).thenReturn(List.of(limit));

        assertThrows(ApiBankException.class, () ->
                accountLimitService.validateTransfer(account, new BigDecimal("1500"), false));
    }

    @Test
    void validateTransfer_dailyLimitExceeded() {
        var account = new Account(); account.setId(1L);
        var limit = new AccountLimit();
        limit.setMaxAmount(new BigDecimal("2000"));
        var lt = new LimitType(1, "DAILY_TRANSFER", null, LimitType.ChangePolicy.BANK_ONLY);
        limit.setLimitType(lt);

        when(accountLimitRepository.findByAccountId(1L)).thenReturn(List.of(limit));
        when(transactionRepository.sumAmountBySourceAccountAndTypeAndStatusBetween(
                eq(1L), eq(TransactionType.TRANSFER), eq(TransactionStatus.COMPLETED), any(), any()))
                .thenReturn(new BigDecimal("1500"));

        assertThrows(ApiBankException.class, () ->
                accountLimitService.validateTransfer(account, new BigDecimal("600"), false));
    }

    @Test
    void validateTransfer_monthlyLimitExceeded() {
        var account = new Account(); account.setId(1L);
        var limit = new AccountLimit();
        limit.setMaxAmount(new BigDecimal("10000"));
        var lt = new LimitType(1, "MONTHLY_TRANSFER", null, LimitType.ChangePolicy.BANK_ONLY);
        limit.setLimitType(lt);

        when(accountLimitRepository.findByAccountId(1L)).thenReturn(List.of(limit));
        when(transactionRepository.sumAmountBySourceAccountAndTypeAndStatusBetween(
                eq(1L), eq(TransactionType.TRANSFER), eq(TransactionStatus.COMPLETED), any(), any()))
                .thenReturn(new BigDecimal("9500"));

        assertThrows(ApiBankException.class, () ->
                accountLimitService.validateTransfer(account, new BigDecimal("600"), false));
    }

    @Test
    void validateTransfer_allLimitsPass() {
        var account = new Account(); account.setId(1L);

        var singleLimit = new AccountLimit();
        singleLimit.setMaxAmount(new BigDecimal("5000"));
        singleLimit.setLimitType(new LimitType(1, "SINGLE_TRANSFER", null, LimitType.ChangePolicy.BANK_ONLY));

        var dailyLimit = new AccountLimit();
        dailyLimit.setMaxAmount(new BigDecimal("2000"));
        dailyLimit.setLimitType(new LimitType(2, "DAILY_TRANSFER", null, LimitType.ChangePolicy.BANK_ONLY));

        var monthlyLimit = new AccountLimit();
        monthlyLimit.setMaxAmount(new BigDecimal("10000"));
        monthlyLimit.setLimitType(new LimitType(3, "MONTHLY_TRANSFER", null, LimitType.ChangePolicy.BANK_ONLY));

        when(accountLimitRepository.findByAccountId(1L)).thenReturn(List.of(singleLimit, dailyLimit, monthlyLimit));
        when(transactionRepository.sumAmountBySourceAccountAndTypeAndStatusBetween(
                eq(1L), eq(TransactionType.TRANSFER), eq(TransactionStatus.COMPLETED), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        assertDoesNotThrow(() ->
                accountLimitService.validateTransfer(account, new BigDecimal("500"), false));
    }

    @Test
    void validateTransfer_noLimitsSet() {
        var account = new Account(); account.setId(1L);
        when(accountLimitRepository.findByAccountId(1L)).thenReturn(List.of());

        assertDoesNotThrow(() ->
                accountLimitService.validateTransfer(account, new BigDecimal("99999"), false));
    }
}
