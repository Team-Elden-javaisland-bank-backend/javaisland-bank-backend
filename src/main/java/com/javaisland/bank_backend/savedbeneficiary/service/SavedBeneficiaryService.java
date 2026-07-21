package com.javaisland.bank_backend.savedbeneficiary.service;

import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.savedbeneficiary.dto.SavedBeneficiaryRequestDto;
import com.javaisland.bank_backend.savedbeneficiary.dto.SavedBeneficiaryResponseDto;
import com.javaisland.bank_backend.savedbeneficiary.model.SavedBeneficiary;
import com.javaisland.bank_backend.savedbeneficiary.repository.SavedBeneficiaryRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedBeneficiaryService {

    private final SavedBeneficiaryRepository savedBeneficiaryRepository;
    private final UserRepository userRepository;

    @Transactional
    public SavedBeneficiaryResponseDto save(Long userId, SavedBeneficiaryRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("User not found.", "USER_NOT_FOUND"));

        SavedBeneficiary sb = new SavedBeneficiary();
        sb.setBeneficiaryName(request.getBeneficiaryName());
        sb.setAccountNumber(request.getAccountNumber());
        sb.setUser(user);

        SavedBeneficiary saved = savedBeneficiaryRepository.save(sb);
        log.info("Saved beneficiary '{}' for user id={}", saved.getBeneficiaryName(), userId);

        return SavedBeneficiaryResponseDto.builder()
                .id(saved.getId())
                .beneficiaryName(saved.getBeneficiaryName())
                .accountNumber(saved.getAccountNumber())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SavedBeneficiaryResponseDto> listByUserId(Long userId) {
        return savedBeneficiaryRepository.findByUserId(userId).stream()
                .map(sb -> SavedBeneficiaryResponseDto.builder()
                        .id(sb.getId())
                        .beneficiaryName(sb.getBeneficiaryName())
                        .accountNumber(sb.getAccountNumber())
                        .build())
                .toList();
    }

    @Transactional
    public void delete(Long userId, Integer id) {
        SavedBeneficiary sb = savedBeneficiaryRepository.findById(id)
                .orElseThrow(() -> new ApiBankException("Saved beneficiary not found.", "BENEFICIARY_NOT_FOUND"));
        if (!sb.getUser().getId().equals(userId)) {
            throw new ApiBankException("Saved beneficiary does not belong to the current user.", "FORBIDDEN");
        }
        savedBeneficiaryRepository.delete(sb);
        log.info("Deleted saved beneficiary id={} for user id={}", id, userId);
    }
}
