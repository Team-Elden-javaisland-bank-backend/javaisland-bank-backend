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
                .orElseThrow(() -> new ApiBankException("Account " + request.getDestinationAccountNumber() + " not found.", "ACCOUNT_NOT_FOUND"));

        if (account.getStatusId() != AccountStatus.ACTIVE) {
            throw new ApiBankException("Destination account is not active.", "INVALID_ACCOUNT_STATE");
        }

        if (beneficiaryRepository.existsByUserIdAndDestinationAccountNumber(userId, request.getDestinationAccountNumber())) {
            throw new ApiBankException("Beneficiary already exists.", "DUPLICATE_BENEFICIARY");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("User not found.", "USER_NOT_FOUND"));

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
                .orElseThrow(() -> new ApiBankException("Beneficiary not found.", "BENEFICIARY_NOT_FOUND"));
        beneficiaryRepository.delete(beneficiary);
    }

    public String resolveAccountNumber(Long userId, Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(() -> new ApiBankException("Beneficiary not found.", "BENEFICIARY_NOT_FOUND"));
        return beneficiary.getDestinationAccountNumber();
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
