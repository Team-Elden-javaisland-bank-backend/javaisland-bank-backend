package com.javaisland.bank_backend.beneficiary.repository;

import com.javaisland.bank_backend.beneficiary.model.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByUserId(Long userId);

    Optional<Beneficiary> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndDestinationAccountNumber(Long userId, String destinationAccountNumber);
}
