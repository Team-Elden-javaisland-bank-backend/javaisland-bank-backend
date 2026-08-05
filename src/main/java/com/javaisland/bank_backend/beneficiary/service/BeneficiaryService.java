package com.javaisland.bank_backend.beneficiary.service;

import com.javaisland.bank_backend.account.model.Account;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public BeneficiaryResponseDto save(Long userId, BeneficiaryRequestDto request) {
        if (beneficiaryRepository.existsByUserIdAndDestinationAccountNumber(userId, request.getDestinationAccountNumber())) {
            throw new ApiBankException("DUPLICATE_BENEFICIARY", "DUPLICATE_BENEFICIARY");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("USER_NOT_FOUND", "USER_NOT_FOUND"));

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setUser(user);
        beneficiary.setNickname(request.getNickname());
        beneficiary.setDestinationAccountNumber(request.getDestinationAccountNumber());
        beneficiary.setBeneficiaryName(request.getBeneficiaryName());

        boolean isExternal = request.getBeneficiaryName() != null && !request.getBeneficiaryName().isBlank();

        if (!isExternal) {
            var account = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                    .orElseThrow(() -> new ApiBankException("ACCOUNT_NOT_FOUND", "ACCOUNT_NOT_FOUND"));

            if (account.getUser().getId().equals(userId)) {
                throw new ApiBankException("SELF_BENEFICIARY_FORBIDDEN", "SELF_BENEFICIARY_FORBIDDEN");
            }

            if (account.getStatusId() != AccountStatus.ACTIVE) {
                throw new ApiBankException("INVALID_ACCOUNT_STATE", "INVALID_ACCOUNT_STATE");
            }
        }

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary '{}' saved for user id={}", saved.getNickname(), userId);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponseDto> listByUserId(Long userId) {
        List<Beneficiary> beneficiaries = beneficiaryRepository.findByUserId(userId);
        List<String> accountNumbers = beneficiaries.stream()
                .map(Beneficiary::getDestinationAccountNumber)
                .toList();
        Map<String, Account> accountMap = accountRepository.findByAccountNumberIn(accountNumbers)
                .stream()
                .collect(Collectors.toMap(Account::getAccountNumber, a -> a));
        return beneficiaries.stream()
                .map(b -> mapToDto(b, accountMap.get(b.getDestinationAccountNumber())))
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(() -> new ApiBankException("BENEFICIARY_NOT_FOUND", "BENEFICIARY_NOT_FOUND"));
        beneficiaryRepository.delete(beneficiary);
        log.info("Deleted beneficiary id={} for user id={}", beneficiaryId, userId);
    }

    public String resolveAccountNumber(Long userId, Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(() -> new ApiBankException("BENEFICIARY_NOT_FOUND", "BENEFICIARY_NOT_FOUND"));
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
                .orElseThrow(() -> new ApiBankException("BENEFICIARY_NOT_FOUND", "BENEFICIARY_NOT_FOUND"));
        beneficiary.setNickname(newNickname);
        return mapToDto(beneficiaryRepository.save(beneficiary));
    }

    private BeneficiaryResponseDto mapToDto(Beneficiary b) {
        Account account = accountRepository.findByAccountNumber(b.getDestinationAccountNumber()).orElse(null);
        return mapToDto(b, account);
    }

    private BeneficiaryResponseDto mapToDto(Beneficiary b, Account account) {
        var builder = BeneficiaryResponseDto.builder()
                .id(b.getId())
                .nickname(b.getNickname())
                .beneficiaryName(b.getBeneficiaryName())
                .destinationAccountNumber(b.getDestinationAccountNumber())
                .createdAt(b.getCreatedAt());

        if (account != null) {
            var holder = account.getUser();
            builder.holderFirstName(holder.getFirstName())
                    .holderLastName(holder.getLastName())
                    .profilePictureUrl(holder.getProfilePictureUrl());
        }

        return builder.build();
    }
}
