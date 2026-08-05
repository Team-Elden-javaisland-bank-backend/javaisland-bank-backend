package com.javaisland.bank_backend.transaction.repository;

import com.javaisland.bank_backend.transaction.model.TransactionStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TransactionStatusRepository extends JpaRepository<TransactionStatus, Integer> {

    @Override
    @Cacheable("transactionStatuses")
    Optional<TransactionStatus> findById(Integer id);

    @Cacheable("transactionStatuses")
    Optional<TransactionStatus> findByStatusName(String statusName);
}
