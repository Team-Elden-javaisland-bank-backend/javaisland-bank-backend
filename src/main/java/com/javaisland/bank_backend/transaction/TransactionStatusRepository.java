package com.javaisland.bank_backend.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TransactionStatusRepository extends JpaRepository<TransactionStatus, Integer> {
    Optional<TransactionStatus> findByStatusName(String statusName);
}
