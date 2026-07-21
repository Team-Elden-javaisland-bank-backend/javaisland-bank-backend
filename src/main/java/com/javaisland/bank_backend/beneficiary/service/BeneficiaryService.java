package com.javaisland.bank_backend.beneficiary.service;

import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.beneficiary.dto.BeneficiaryRequestDto;
import com.javaisland.bank_backend.beneficiary.dto.BeneficiaryResponseDto;
import com.javaisland.bank_backend.beneficiary.model.Beneficiary;
import com.javaisland.bank_backend.beneficiary.repository.BeneficiaryRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public BeneficiaryResponseDto save(Long userId, BeneficiaryRequestDto request) {
        var account = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new ApiBankException("Conto " + request.getDestinationAccountNumber() + " non trovato.", "ACCOUNT_NOT_FOUND"));

        if (account.getUser().getId().equals(userId)) {
            throw new ApiBankException("Non puoi aggiungere uno dei tuoi conti come beneficiario.", "SELF_BENEFICIARY_FORBIDDEN");
        }

        if (account.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("Il conto di destinazione non è attivo.", "INVALID_ACCOUNT_STATE");
        }

        if (beneficiaryRepository.existsByUserIdAndDestinationAccountNumber(userId, request.getDestinationAccountNumber())) {
            throw new ApiBankException("Il beneficiario esiste già.", "DUPLICATE_BENEFICIARY");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setUser(user);
        beneficiary.setNickname(request.getNickname());
        beneficiary.setDestinationAccountNumber(request.getDestinationAccountNumber());

        return mapToDto(beneficiaryRepository.save(beneficiary));
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponseDto> listByUserId(Long userId) {
        return beneficiaryRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(() -> new ApiBankException("Beneficiario non trovato.", "BENEFICIARY_NOT_FOUND"));
        beneficiaryRepository.delete(beneficiary);
    }

    public String resolveAccountNumber(Long userId, Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(() -> new ApiBankException("Beneficiario non trovato.", "BENEFICIARY_NOT_FOUND"));
        return beneficiary.getDestinationAccountNumber();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponseDto findByAccountNumber(Long userId, String accountNumber) {
        return beneficiaryRepository.findByUserIdAndDestinationAccountNumber(userId, accountNumber)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Transactional
    public BeneficiaryResponseDto rename(Long userId, Long beneficiaryId, String newNickname) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(() -> new ApiBankException("Beneficiario non trovato.", "BENEFICIARY_NOT_FOUND"));
        beneficiary.setNickname(newNickname);
        return mapToDto(beneficiaryRepository.save(beneficiary));
    }

    private BeneficiaryResponseDto mapToDto(Beneficiary b) {
        return BeneficiaryResponseDto.builder()
                .id(b.getId())
                .nickname(b.getNickname())
                .destinationAccountNumber(b.getDestinationAccountNumber())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
