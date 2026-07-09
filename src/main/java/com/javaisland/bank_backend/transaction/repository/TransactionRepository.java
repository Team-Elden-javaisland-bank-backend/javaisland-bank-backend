package com.javaisland.bank_backend.transaction.repository;

import com.javaisland.bank_backend.transaction.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findBySourceAccount_IdOrDestinationAccount_IdOrderByCreatedAtDesc(
            Long sourceAccountId, Long destinationAccountId, Pageable pageable);
}
