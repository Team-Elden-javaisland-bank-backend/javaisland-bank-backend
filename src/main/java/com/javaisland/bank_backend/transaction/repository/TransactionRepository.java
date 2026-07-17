package com.javaisland.bank_backend.transaction.repository;

import com.javaisland.bank_backend.transaction.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findBySourceAccount_IdOrDestinationAccount_IdOrderByCreatedAtDesc(
            Long sourceAccountId, Long destinationAccountId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.sourceAccount.id = :accountId " +
           "AND t.typeId = :typeId " +
           "AND t.statusId = :statusId " +
           "AND t.createdAt BETWEEN :start AND :end")
    BigDecimal sumAmountBySourceAccountAndTypeAndStatusBetween(
            @Param("accountId") Long accountId,
            @Param("typeId") Integer typeId,
            @Param("statusId") Integer statusId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<Transaction> findByStatusIdAndScheduledDateLessThanEqual(Integer statusId, LocalDateTime scheduledDate);
}
