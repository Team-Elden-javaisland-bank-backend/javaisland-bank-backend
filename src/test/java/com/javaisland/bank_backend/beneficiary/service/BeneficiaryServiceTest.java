package com.javaisland.bank_backend.beneficiary.service;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.beneficiary.dto.BeneficiaryRequestDto;
import com.javaisland.bank_backend.beneficiary.model.Beneficiary;
import com.javaisland.bank_backend.beneficiary.repository.BeneficiaryRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private BeneficiaryService beneficiaryService;

    @Test
    void save_createsBeneficiary() {
        var dto = new BeneficiaryRequestDto();
        dto.setNickname("Mamma");
        dto.setDestinationAccountNumber("IT999");

        var accountOwner = new User();
        accountOwner.setId(2L);

        var account = new Account();
        account.setStatusId(AccountStatus.ACTIVE);
        account.setUser(accountOwner);

        var user = new User();
        user.setId(1L);

        var saved = new Beneficiary();
        saved.setId(1L);
        saved.setNickname("Mamma");
        saved.setDestinationAccountNumber("IT999");

        when(accountRepository.findByAccountNumber("IT999")).thenReturn(Optional.of(account));
        when(beneficiaryRepository.existsByUserIdAndDestinationAccountNumber(1L, "IT999")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(beneficiaryRepository.save(any())).thenReturn(saved);

        var result = beneficiaryService.save(1L, dto);

        assertEquals("Mamma", result.getNickname());
        assertEquals("IT999", result.getDestinationAccountNumber());
    }

    @Test
    void save_accountNotFound() {
        var dto = new BeneficiaryRequestDto();
        dto.setDestinationAccountNumber("IT999");

        when(accountRepository.findByAccountNumber("IT999")).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> beneficiaryService.save(1L, dto));
    }

    @Test
    void save_accountNotActive() {
        var dto = new BeneficiaryRequestDto();
        dto.setDestinationAccountNumber("IT999");

        var accountOwner = new User();
        accountOwner.setId(2L);

        var account = new Account();
        account.setStatusId(AccountStatus.INACTIVE);
        account.setUser(accountOwner);

        when(accountRepository.findByAccountNumber("IT999")).thenReturn(Optional.of(account));

        assertThrows(ApiBankException.class, () -> beneficiaryService.save(1L, dto));
    }

    @Test
    void save_duplicateThrows() {
        var dto = new BeneficiaryRequestDto();
        dto.setDestinationAccountNumber("IT999");

        var accountOwner = new User();
        accountOwner.setId(2L);

        var account = new Account();
        account.setStatusId(AccountStatus.ACTIVE);
        account.setUser(accountOwner);

        when(accountRepository.findByAccountNumber("IT999")).thenReturn(Optional.of(account));
        when(beneficiaryRepository.existsByUserIdAndDestinationAccountNumber(1L, "IT999")).thenReturn(true);

        assertThrows(ApiBankException.class, () -> beneficiaryService.save(1L, dto));
    }

    @Test
    void listByUserId_returnsList() {
        var b = new Beneficiary();
        b.setNickname("Mamma");
        b.setDestinationAccountNumber("IT999");

        when(beneficiaryRepository.findByUserId(1L)).thenReturn(List.of(b));

        var results = beneficiaryService.listByUserId(1L);

        assertEquals(1, results.size());
        assertEquals("Mamma", results.get(0).getNickname());
    }

    @Test
    void delete_removesBeneficiary() {
        var b = new Beneficiary();
        b.setId(1L);

        when(beneficiaryRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.of(b));

        beneficiaryService.delete(99L, 1L);

        verify(beneficiaryRepository).delete(b);
    }

    @Test
    void delete_notFoundThrows() {
        when(beneficiaryRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> beneficiaryService.delete(99L, 1L));
    }

    @Test
    void resolveAccountNumber_returnsNumber() {
        var b = new Beneficiary();
        b.setDestinationAccountNumber("IT999");

        when(beneficiaryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(b));

        var result = beneficiaryService.resolveAccountNumber(1L, 1L);

        assertEquals("IT999", result);
    }

    @Test
    void resolveAccountNumber_notFoundThrows() {
        when(beneficiaryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> beneficiaryService.resolveAccountNumber(1L, 1L));
    }
}
