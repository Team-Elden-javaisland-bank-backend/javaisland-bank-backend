package com.javaisland.bank_backend.account.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.audit.service.AuditLogService;
import com.javaisland.bank_backend.card.service.CardService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountManagementServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardService cardService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private IbanGenerator ibanGenerator;

    @InjectMocks private AccountManagementService accountManagementService;

    private Account activeAccount(String number, User user) {
        Account a = new Account();
        a.setId(1L);
        a.setAccountNumber(number);
        a.setStatusId(AccountStatus.ACTIVE);
        a.setUser(user);
        a.setIsLimitsConfigured(false);
        return a;
    }

    @Test
    void markLimitsConfigured_setsFlagOnOwnedActiveAccount() {
        User user = new User();
        user.setId(10L);
        Account account = activeAccount("IT123", user);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));

        accountManagementService.markLimitsConfigured(10L, "IT123");

        assertTrue(account.getIsLimitsConfigured());
        verify(accountRepository).save(account);
    }

    @Test
    void markLimitsConfigured_rejectsForeignAccount() {
        User owner = new User();
        owner.setId(10L);
        User attacker = new User();
        attacker.setId(99L);
        Account account = activeAccount("IT123", owner);

        when(userRepository.findById(99L)).thenReturn(Optional.of(attacker));
        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));

        assertThrows(ApiBankException.class, () -> accountManagementService.markLimitsConfigured(99L, "IT123"));
        assertFalse(account.getIsLimitsConfigured());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void markLimitsConfigured_rejectsNonActiveAccount() {
        User user = new User();
        user.setId(10L);
        Account account = activeAccount("IT123", user);
        account.setStatusId(AccountStatus.INACTIVE);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));

        assertThrows(ApiBankException.class, () -> accountManagementService.markLimitsConfigured(10L, "IT123"));
        assertFalse(account.getIsLimitsConfigured());
    }

    @Test
    void completeLimitsSetup_marksUserAndNonClosedAccounts() {
        User user = new User();
        user.setId(10L);
        user.setLimitsSetupComplete(false);

        Account active = activeAccount("IT1", user);
        Account closed = activeAccount("IT2", user);
        closed.setStatusId(AccountStatus.CLOSED);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(accountRepository.findByUserId(10L)).thenReturn(List.of(active, closed));

        accountManagementService.completeLimitsSetup(10L);

        assertTrue(user.isLimitsSetupComplete());
        assertTrue(active.getIsLimitsConfigured());
        assertFalse(closed.getIsLimitsConfigured());
        verify(userRepository).save(user);
        verify(accountRepository).save(active);
    }

    @Test
    void requestClosure_marksAccountFrozenWithClosureRequestedAt() {
        User user = new User();
        user.setId(10L);
        Account target = activeAccount("IT1", user);
        Account other = activeAccount("IT2", user);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("IT1")).thenReturn(Optional.of(target));
        when(accountRepository.findByUserId(10L)).thenReturn(List.of(target, other));

        accountManagementService.requestClosure(10L, "IT1");

        assertEquals(AccountStatus.FROZEN, target.getStatusId());
        assertNotNull(target.getClosureRequestedAt());
        verify(accountRepository).save(target);
        verify(cardService).blockCardsByAccountId(target.getId());
    }

    @Test
    void createInitialAccount_startsWithLimitsNotConfigured() {
        User user = new User();
        user.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(ibanGenerator.generate()).thenReturn("ITNEW");
        when(accountRepository.existsByAccountNumber("ITNEW")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account created = accountManagementService.createInitialAccountForUser(user);

        assertEquals(AccountStatus.INACTIVE, created.getStatusId());
        assertFalse(created.getIsLimitsConfigured());
    }
}
