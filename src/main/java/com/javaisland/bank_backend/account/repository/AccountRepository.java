package com.javaisland.bank_backend.account.repository;

import com.javaisland.bank_backend.account.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    List<Account> findByUserId(Long userId);

    List<Account> findByUserIdIn(List<Long> userIds);

    List<Account> findByAccountNumberIn(List<String> accountNumbers);

    List<Account> findByStatusId(Integer statusId);

    boolean existsByAccountNumber(String accountNumber);

    long countByStatusId(Integer statusId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.statusId = :statusId")
    BigDecimal sumBalanceByStatusId(@Param("statusId") Integer statusId);
}
