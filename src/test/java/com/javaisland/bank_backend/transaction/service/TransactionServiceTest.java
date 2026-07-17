package com.javaisland.bank_backend.transaction.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.service.AccountLimitService;
import com.javaisland.bank_backend.beneficiary.service.BeneficiaryService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.transaction.dto.TransferRequestDto;
import com.javaisland.bank_backend.transaction.model.Transaction;
import com.javaisland.bank_backend.transaction.model.TransactionStatus;
import com.javaisland.bank_backend.transaction.model.TransactionType;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import com.javaisland.bank_backend.transaction.repository.TransactionStatusRepository;
import com.javaisland.bank_backend.transaction.repository.TransactionTypeRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionStatusRepository transactionStatusRepository;
    @Mock private TransactionTypeRepository transactionTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private BeneficiaryService beneficiaryService;
    @Mock private AccountLimitService accountLimitService;

    @InjectMocks private TransactionService transactionService;

    private Account createAccount(Long id, String number, BigDecimal balance, int statusId, Long userId) {
        Account a = new Account();
        a.setId(id);
        a.setAccountNumber(number);
        a.setBalance(balance);
        a.setStatusId(statusId);
        User u = new User();
        u.setId(userId);
        a.setUser(u);
        return a;
    }

    @Test
    void transferFunds_fullTransfer() {
        when(transactionTypeRepository.findByTypeName("TRANSFER"))
                .thenReturn(Optional.of(new TransactionType(3, "TRANSFER")));
        when(transactionStatusRepository.findByStatusName("COMPLETED"))
                .thenReturn(Optional.of(new TransactionStatus(2, "COMPLETED")));

        Account src = createAccount(1L, "IT1", new BigDecimal("1000"), AccountStatus.ACTIVE, 10L);
        Account dst = createAccount(2L, "IT2", new BigDecimal("500"), AccountStatus.ACTIVE, 20L);

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Transaction tx = transactionService.transferFunds(src, dst, new BigDecimal("300"), "TRANSFER", "COMPLETED", "Test");

        assertEquals(0, new BigDecimal("700").compareTo(tx.getSourceBalanceAfter()));
        assertEquals(0, new BigDecimal("800").compareTo(tx.getDestBalanceAfter()));
        verify(accountRepository, times(2)).save(any());
    }

    @Test
    void transferFunds_deposit() {
        when(transactionTypeRepository.findByTypeName("DEPOSIT"))
                .thenReturn(Optional.of(new TransactionType(1, "DEPOSIT")));
        when(transactionStatusRepository.findByStatusName("COMPLETED"))
                .thenReturn(Optional.of(new TransactionStatus(2, "COMPLETED")));

        Account dst = createAccount(1L, "IT1", new BigDecimal("500"), AccountStatus.ACTIVE, 10L);

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Transaction tx = transactionService.transferFunds(null, dst, new BigDecimal("300"), "DEPOSIT", "COMPLETED", "Deposit");

        assertNull(tx.getSourceBalanceAfter());
        assertEquals(0, new BigDecimal("800").compareTo(tx.getDestBalanceAfter()));
    }

    @Test
    void transferFunds_withdraw() {
        when(transactionTypeRepository.findByTypeName("WITHDRAWAL"))
                .thenReturn(Optional.of(new TransactionType(2, "WITHDRAWAL")));
        when(transactionStatusRepository.findByStatusName("COMPLETED"))
                .thenReturn(Optional.of(new TransactionStatus(2, "COMPLETED")));

        Account src = createAccount(1L, "IT1", new BigDecimal("1000"), AccountStatus.ACTIVE, 10L);

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Transaction tx = transactionService.transferFunds(src, null, new BigDecimal("400"), "WITHDRAWAL", "COMPLETED", "Withdrawal");

        assertEquals(0, new BigDecimal("600").compareTo(tx.getSourceBalanceAfter()));
        assertNull(tx.getDestBalanceAfter());
    }

    @Test
    void transferFunds_insufficientFunds() {
        when(transactionTypeRepository.findByTypeName("WITHDRAWAL"))
                .thenReturn(Optional.of(new TransactionType(2, "WITHDRAWAL")));
        when(transactionStatusRepository.findByStatusName("COMPLETED"))
                .thenReturn(Optional.of(new TransactionStatus(2, "COMPLETED")));

        Account src = createAccount(1L, "IT1", new BigDecimal("100"), AccountStatus.ACTIVE, 10L);

        assertThrows(ApiBankException.class, () ->
                transactionService.transferFunds(src, null, new BigDecimal("200"), "WITHDRAWAL", "COMPLETED", "Test"));
    }

    @Test
    void transferFunds_inactiveSource() {
        when(transactionTypeRepository.findByTypeName("WITHDRAWAL"))
                .thenReturn(Optional.of(new TransactionType(2, "WITHDRAWAL")));
        when(transactionStatusRepository.findByStatusName("COMPLETED"))
                .thenReturn(Optional.of(new TransactionStatus(2, "COMPLETED")));

        Account src = createAccount(1L, "IT1", new BigDecimal("1000"), AccountStatus.INACTIVE, 10L);

        assertThrows(ApiBankException.class, () ->
                transactionService.transferFunds(src, null, new BigDecimal("100"), "WITHDRAWAL", "COMPLETED", "Test"));
    }

    @Test
    void transferFunds_inactiveDestination() {
        when(transactionTypeRepository.findByTypeName("TRANSFER"))
                .thenReturn(Optional.of(new TransactionType(3, "TRANSFER")));
        when(transactionStatusRepository.findByStatusName("COMPLETED"))
                .thenReturn(Optional.of(new TransactionStatus(2, "COMPLETED")));

        Account src = createAccount(1L, "IT1", new BigDecimal("1000"), AccountStatus.ACTIVE, 10L);
        Account dst = createAccount(2L, "IT2", new BigDecimal("500"), AccountStatus.INACTIVE, 20L);

        assertThrows(ApiBankException.class, () ->
                transactionService.transferFunds(src, dst, new BigDecimal("100"), "TRANSFER", "COMPLETED", "Test"));
    }

    @Test
    void transferFunds_zeroAmount() {
        assertThrows(ApiBankException.class, () ->
                transactionService.transferFunds(null, null, BigDecimal.ZERO, "DEPOSIT", "COMPLETED", "Test"));
    }

    @Test
    void transferFunds_bothNullAccounts() {
        assertThrows(ApiBankException.class, () ->
                transactionService.transferFunds(null, null, new BigDecimal("100"), "DEPOSIT", "COMPLETED", "Test"));
    }

    @Test
    void transfer_happyPath() {
        when(transactionTypeRepository.findByTypeName("TRANSFER"))
                .thenReturn(Optional.of(new TransactionType(3, "TRANSFER")));
        when(transactionStatusRepository.findByStatusName("PENDING"))
                .thenReturn(Optional.of(new TransactionStatus(1, "PENDING")));

        User user = new User(); user.setId(10L);

        Account src = createAccount(1L, "IT1", new BigDecimal("1000"), AccountStatus.ACTIVE, 10L);
        Account dst = createAccount(2L, "IT2", new BigDecimal("500"), AccountStatus.ACTIVE, 20L);

        var dto = new TransferRequestDto();
        dto.setSourceAccountNumber("IT1");
        dto.setDestinationAccountNumber("IT2");
        dto.setAmount(new BigDecimal("300"));
        dto.setScheduledDate(LocalDate.now().plusDays(2));

        when(accountRepository.findByAccountNumber("IT1")).thenReturn(Optional.of(src));
        when(accountRepository.findByAccountNumber("IT2")).thenReturn(Optional.of(dst));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = transactionService.transfer(10L, dto);

        assertNotNull(result);
    }

    @Test
    void transfer_viaBeneficiary() {
        when(transactionTypeRepository.findByTypeName("TRANSFER"))
                .thenReturn(Optional.of(new TransactionType(3, "TRANSFER")));
        when(transactionStatusRepository.findByStatusName("PENDING"))
                .thenReturn(Optional.of(new TransactionStatus(1, "PENDING")));

        User user = new User(); user.setId(10L);

        Account src = createAccount(1L, "IT1", new BigDecimal("1000"), AccountStatus.ACTIVE, 10L);
        Account dst = createAccount(2L, "IT2", new BigDecimal("500"), AccountStatus.ACTIVE, 20L);

        var dto = new TransferRequestDto();
        dto.setSourceAccountNumber("IT1");
        dto.setBeneficiaryId(99L);
        dto.setAmount(new BigDecimal("300"));
        dto.setScheduledDate(LocalDate.now().plusDays(2));

        when(beneficiaryService.resolveAccountNumber(10L, 99L)).thenReturn("IT2");
        when(accountRepository.findByAccountNumber("IT1")).thenReturn(Optional.of(src));
        when(accountRepository.findByAccountNumber("IT2")).thenReturn(Optional.of(dst));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = transactionService.transfer(10L, dto);

        assertNotNull(result);
        verify(beneficiaryService).resolveAccountNumber(10L, 99L);
    }

    @Test
    void transfer_sourceEqualsDest() {
        var dto = new TransferRequestDto();
        dto.setSourceAccountNumber("IT1");
        dto.setDestinationAccountNumber("IT1");
        dto.setAmount(new BigDecimal("100"));

        assertThrows(ApiBankException.class, () -> transactionService.transfer(10L, dto));
    }

    @Test
    void transfer_noDestination() {
        var dto = new TransferRequestDto();
        dto.setSourceAccountNumber("IT1");
        dto.setAmount(new BigDecimal("100"));

        assertThrows(ApiBankException.class, () -> transactionService.transfer(10L, dto));
    }

    @Test
    void deposit_happyPath() {
        when(transactionTypeRepository.findByTypeName("DEPOSIT"))
                .thenReturn(Optional.of(new TransactionType(1, "DEPOSIT")));
        when(transactionStatusRepository.findByStatusName("COMPLETED"))
                .thenReturn(Optional.of(new TransactionStatus(2, "COMPLETED")));

        User user = new User(); user.setId(10L);
        Account acc = createAccount(1L, "IT1", new BigDecimal("500"), AccountStatus.ACTIVE, 10L);

        var request = new com.javaisland.bank_backend.transaction.dto.TransactionRequestDto();
        request.setAccountNumber("IT1");
        request.setAmount(new BigDecimal("200"));

        when(accountRepository.findByAccountNumber("IT1")).thenReturn(Optional.of(acc));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transactionService.deposit(10L, request);

        assertEquals(0, new BigDecimal("700").compareTo(acc.getBalance()));
    }

    @Test
    void withdraw_happyPath() {
        when(transactionTypeRepository.findByTypeName("WITHDRAWAL"))
                .thenReturn(Optional.of(new TransactionType(2, "WITHDRAWAL")));
        when(transactionStatusRepository.findByStatusName("COMPLETED"))
                .thenReturn(Optional.of(new TransactionStatus(2, "COMPLETED")));

        User user = new User(); user.setId(10L);
        Account acc = createAccount(1L, "IT1", new BigDecimal("1000"), AccountStatus.ACTIVE, 10L);

        var request = new com.javaisland.bank_backend.transaction.dto.TransactionRequestDto();
        request.setAccountNumber("IT1");
        request.setAmount(new BigDecimal("400"));

        when(accountRepository.findByAccountNumber("IT1")).thenReturn(Optional.of(acc));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transactionService.withdraw(10L, request);

        assertEquals(0, new BigDecimal("600").compareTo(acc.getBalance()));
    }

    @Test
    void withdraw_insufficientFunds() {
        when(transactionTypeRepository.findByTypeName("WITHDRAWAL"))
                .thenReturn(Optional.of(new TransactionType(2, "WITHDRAWAL")));
        when(transactionStatusRepository.findByStatusName("COMPLETED"))
                .thenReturn(Optional.of(new TransactionStatus(2, "COMPLETED")));

        User user = new User(); user.setId(10L);
        Account acc = createAccount(1L, "IT1", new BigDecimal("100"), AccountStatus.ACTIVE, 10L);

        var request = new com.javaisland.bank_backend.transaction.dto.TransactionRequestDto();
        request.setAccountNumber("IT1");
        request.setAmount(new BigDecimal("500"));

        when(accountRepository.findByAccountNumber("IT1")).thenReturn(Optional.of(acc));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThrows(ApiBankException.class, () -> transactionService.withdraw(10L, request));
    }
}
