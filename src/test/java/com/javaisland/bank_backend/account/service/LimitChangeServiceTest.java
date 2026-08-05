package com.javaisland.bank_backend.account.service;

import com.javaisland.bank_backend.account.dto.LimitChangeRequestCreateDto;
import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountLimit;
import com.javaisland.bank_backend.account.model.LimitChangeRequest;
import com.javaisland.bank_backend.account.model.LimitType;
import com.javaisland.bank_backend.account.repository.AccountLimitRepository;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.repository.LimitChangeRequestRepository;
import com.javaisland.bank_backend.account.repository.LimitTypeRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LimitChangeServiceTest {

    @Mock private LimitChangeRequestRepository limitChangeRequestRepository;
    @Mock private AccountLimitRepository accountLimitRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private LimitTypeRepository limitTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AccountLimitService accountLimitService;

    @InjectMocks private LimitChangeService limitChangeService;

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Account createAccount(String accountNumber, User user) {
        Account account = new Account();
        account.setId(1L);
        account.setAccountNumber(accountNumber);
        account.setUser(user);
        return account;
    }

    private LimitType createLimitType(String name, LimitType.ChangePolicy policy) {
        LimitType limitType = new LimitType();
        limitType.setId(1);
        limitType.setLimitName(name);
        limitType.setChangePolicy(policy);
        return limitType;
    }

    private LimitChangeRequest createPendingRequest(Long id, Long userId, String accountNumber, String limitTypeName) {
        return LimitChangeRequest.builder()
                .id(id)
                .userId(userId)
                .accountNumber(accountNumber)
                .limitTypeName(limitTypeName)
                .requestedAmount(new BigDecimal("5000"))
                .currentAmount(BigDecimal.ZERO)
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void requestLimitChange_success() {
        Long userId = 1L;
        User user = createUser(userId);
        Account account = createAccount("IT123", user);
        LimitType limitType = createLimitType("SINGLE_TRANSFER", LimitType.ChangePolicy.BANK_ONLY);
        LimitChangeRequestCreateDto dto = LimitChangeRequestCreateDto.builder()
                .accountNumber("IT123")
                .limitType("SINGLE_TRANSFER")
                .requestedAmount(new BigDecimal("5000"))
                .build();

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("SINGLE_TRANSFER")).thenReturn(Optional.of(limitType));
        when(accountLimitRepository.findByAccountAndLimitType(account, limitType)).thenReturn(Optional.empty());
        when(limitChangeRequestRepository.existsByAccountNumberAndLimitTypeNameAndStatus("IT123", "SINGLE_TRANSFER", "PENDING"))
                .thenReturn(false);

        limitChangeService.requestLimitChange(userId, dto);

        verify(limitChangeRequestRepository).save(any(LimitChangeRequest.class));
        verify(notificationService).send(eq(userId), eq("LIMIT_CHANGE"), anyString(),
                eq("NOTIF_LIMIT_CHANGE_REQUESTED"), anyString());
    }

    @Test
    void requestLimitChange_accountNotFound() {
        Long userId = 1L;
        LimitChangeRequestCreateDto dto = LimitChangeRequestCreateDto.builder()
                .accountNumber("IT999")
                .limitType("SINGLE_TRANSFER")
                .requestedAmount(new BigDecimal("5000"))
                .build();

        when(accountRepository.findByAccountNumber("IT999")).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> limitChangeService.requestLimitChange(userId, dto));
        verify(limitChangeRequestRepository, never()).save(any());
    }

    @Test
    void requestLimitChange_accountNotOwned() {
        Long userId = 1L;
        User otherUser = createUser(2L);
        Account account = createAccount("IT123", otherUser);
        LimitChangeRequestCreateDto dto = LimitChangeRequestCreateDto.builder()
                .accountNumber("IT123")
                .limitType("SINGLE_TRANSFER")
                .requestedAmount(new BigDecimal("5000"))
                .build();

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));

        assertThrows(ApiBankException.class, () -> limitChangeService.requestLimitChange(userId, dto));
        verify(limitChangeRequestRepository, never()).save(any());
    }

    @Test
    void requestLimitChange_limitTypeNotFound() {
        Long userId = 1L;
        User user = createUser(userId);
        Account account = createAccount("IT123", user);
        LimitChangeRequestCreateDto dto = LimitChangeRequestCreateDto.builder()
                .accountNumber("IT123")
                .limitType("UNKNOWN_TYPE")
                .requestedAmount(new BigDecimal("5000"))
                .build();

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("UNKNOWN_TYPE")).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> limitChangeService.requestLimitChange(userId, dto));
        verify(limitChangeRequestRepository, never()).save(any());
    }

    @Test
    void requestLimitChange_userFullPolicy() {
        Long userId = 1L;
        User user = createUser(userId);
        Account account = createAccount("IT123", user);
        LimitType limitType = createLimitType("ATM_WITHDRAWAL", LimitType.ChangePolicy.USER_FULL);
        LimitChangeRequestCreateDto dto = LimitChangeRequestCreateDto.builder()
                .accountNumber("IT123")
                .limitType("ATM_WITHDRAWAL")
                .requestedAmount(new BigDecimal("500"))
                .build();

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("ATM_WITHDRAWAL")).thenReturn(Optional.of(limitType));
        when(accountLimitRepository.findByAccountAndLimitType(account, limitType)).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> limitChangeService.requestLimitChange(userId, dto));
        verify(limitChangeRequestRepository, never()).save(any());
    }

    @Test
    void requestLimitChange_pendingRequestExists() {
        Long userId = 1L;
        User user = createUser(userId);
        Account account = createAccount("IT123", user);
        LimitType limitType = createLimitType("SINGLE_TRANSFER", LimitType.ChangePolicy.BANK_ONLY);
        LimitChangeRequestCreateDto dto = LimitChangeRequestCreateDto.builder()
                .accountNumber("IT123")
                .limitType("SINGLE_TRANSFER")
                .requestedAmount(new BigDecimal("5000"))
                .build();

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("SINGLE_TRANSFER")).thenReturn(Optional.of(limitType));
        when(accountLimitRepository.findByAccountAndLimitType(account, limitType)).thenReturn(Optional.empty());
        when(limitChangeRequestRepository.existsByAccountNumberAndLimitTypeNameAndStatus("IT123", "SINGLE_TRANSFER", "PENDING"))
                .thenReturn(true);

        assertThrows(ApiBankException.class, () -> limitChangeService.requestLimitChange(userId, dto));
        verify(limitChangeRequestRepository, never()).save(any());
    }

    @Test
    void requestLimitChange_userLowerOnly_decrease_appliedDirectly() {
        Long userId = 1L;
        User user = createUser(userId);
        Account account = createAccount("IT123", user);
        LimitType limitType = createLimitType("DAILY_TRANSFER", LimitType.ChangePolicy.USER_LOWER_ONLY);
        AccountLimit existing = AccountLimit.builder()
                .account(account)
                .limitType(limitType)
                .maxAmount(new BigDecimal("10000"))
                .build();
        LimitChangeRequestCreateDto dto = LimitChangeRequestCreateDto.builder()
                .accountNumber("IT123")
                .limitType("DAILY_TRANSFER")
                .requestedAmount(new BigDecimal("5000"))
                .build();

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("DAILY_TRANSFER")).thenReturn(Optional.of(limitType));
        when(accountLimitRepository.findByAccountAndLimitType(account, limitType)).thenReturn(Optional.of(existing));

        limitChangeService.requestLimitChange(userId, dto);

        assertEquals(new BigDecimal("5000"), existing.getMaxAmount());
        verify(accountLimitRepository).save(existing);
        verify(limitChangeRequestRepository, never()).save(any());
        verify(notificationService).send(eq(userId), eq("LIMIT_CHANGE"), anyString(),
                eq("NOTIF_LIMIT_CHANGE_DECREASED"), anyString());
    }

    @Test
    void requestLimitChange_userLowerOnly_increase_createsRequest() {
        Long userId = 1L;
        User user = createUser(userId);
        Account account = createAccount("IT123", user);
        LimitType limitType = createLimitType("DAILY_TRANSFER", LimitType.ChangePolicy.USER_LOWER_ONLY);
        AccountLimit existing = AccountLimit.builder()
                .account(account)
                .limitType(limitType)
                .maxAmount(new BigDecimal("1000"))
                .build();
        LimitChangeRequestCreateDto dto = LimitChangeRequestCreateDto.builder()
                .accountNumber("IT123")
                .limitType("DAILY_TRANSFER")
                .requestedAmount(new BigDecimal("5000"))
                .build();

        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("DAILY_TRANSFER")).thenReturn(Optional.of(limitType));
        when(accountLimitRepository.findByAccountAndLimitType(account, limitType)).thenReturn(Optional.of(existing));
        when(limitChangeRequestRepository.existsByAccountNumberAndLimitTypeNameAndStatus("IT123", "DAILY_TRANSFER", "PENDING"))
                .thenReturn(false);

        limitChangeService.requestLimitChange(userId, dto);

        assertEquals(new BigDecimal("1000"), existing.getMaxAmount());
        verify(limitChangeRequestRepository).save(any(LimitChangeRequest.class));
        verify(notificationService).send(eq(userId), eq("LIMIT_CHANGE"), anyString(),
                eq("NOTIF_LIMIT_CHANGE_REQUESTED"), anyString());
    }

    @Test
    void approveRequest_success() {
        Long requestId = 1L;
        Long userId = 1L;
        User user = createUser(userId);
        Account account = createAccount("IT123", user);
        LimitType limitType = createLimitType("SINGLE_TRANSFER", LimitType.ChangePolicy.BANK_ONLY);
        LimitChangeRequest request = createPendingRequest(requestId, userId, "IT123", "SINGLE_TRANSFER");

        when(limitChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(accountRepository.findByAccountNumber("IT123")).thenReturn(Optional.of(account));
        when(limitTypeRepository.findByLimitName("SINGLE_TRANSFER")).thenReturn(Optional.of(limitType));
        when(accountLimitRepository.findByAccountAndLimitType(account, limitType)).thenReturn(Optional.empty());

        limitChangeService.approveRequest(requestId);

        assertEquals("APPROVED", request.getStatus());
        assertNotNull(request.getProcessedAt());
        verify(accountLimitRepository).save(any(AccountLimit.class));
        verify(limitChangeRequestRepository).save(request);
        verify(notificationService).send(eq(userId), eq("LIMIT_CHANGE"), anyString(),
                eq("NOTIF_LIMIT_CHANGE_APPROVED"), anyString());
    }

    @Test
    void approveRequest_requestNotFound() {
        Long requestId = 1L;

        when(limitChangeRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> limitChangeService.approveRequest(requestId));
        verify(limitChangeRequestRepository, never()).save(any());
    }

    @Test
    void approveRequest_requestNotPending() {
        Long requestId = 1L;
        Long userId = 1L;
        LimitChangeRequest request = LimitChangeRequest.builder()
                .id(requestId)
                .userId(userId)
                .status("APPROVED")
                .accountNumber("IT123")
                .limitTypeName("SINGLE_TRANSFER")
                .requestedAmount(new BigDecimal("5000"))
                .currentAmount(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now())
                .build();

        when(limitChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ApiBankException.class, () -> limitChangeService.approveRequest(requestId));
        verify(limitChangeRequestRepository, never()).save(request);
    }

    @Test
    void rejectRequest_success() {
        Long requestId = 1L;
        Long userId = 1L;
        LimitChangeRequest request = createPendingRequest(requestId, userId, "IT123", "SINGLE_TRANSFER");

        when(limitChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        limitChangeService.rejectRequest(requestId);

        assertEquals("REJECTED", request.getStatus());
        assertNotNull(request.getProcessedAt());
        verify(limitChangeRequestRepository).save(request);
        verify(notificationService).send(eq(userId), eq("LIMIT_CHANGE"), anyString(),
                eq("NOTIF_LIMIT_CHANGE_REJECTED"), anyString());
    }
}
