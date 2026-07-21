package com.javaisland.bank_backend.savedbeneficiary.repository;

import com.javaisland.bank_backend.savedbeneficiary.model.SavedBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedBeneficiaryRepository extends JpaRepository<SavedBeneficiary, Integer> {
    List<SavedBeneficiary> findByUserId(Long userId);
}
