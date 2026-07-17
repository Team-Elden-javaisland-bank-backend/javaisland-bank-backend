package com.javaisland.bank_backend.account.repository;

import com.javaisland.bank_backend.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserId(Long userId);

    List<Account> findByStatusId(Integer statusId);

    boolean existsByAccountNumber(String accountNumber);

    long countByStatusId(Integer statusId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.statusId = :statusId")
    BigDecimal sumBalanceByStatusId(@Param("statusId") Integer statusId);
}
