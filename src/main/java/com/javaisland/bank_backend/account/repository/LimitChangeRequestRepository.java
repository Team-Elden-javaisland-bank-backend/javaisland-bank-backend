package com.javaisland.bank_backend.account.repository;

import com.javaisland.bank_backend.account.model.LimitChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LimitChangeRequestRepository extends JpaRepository<LimitChangeRequest, Long> {

    List<LimitChangeRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<LimitChangeRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByAccountNumberAndLimitTypeNameAndStatus(String accountNumber, String limitTypeName, String status);
}
