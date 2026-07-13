package com.javaisland.bank_backend.account.repository;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.AccountLimit;
import com.javaisland.bank_backend.account.model.LimitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountLimitRepository extends JpaRepository<AccountLimit, Long> {

    List<AccountLimit> findByAccountId(Long accountId);

    Optional<AccountLimit> findByAccountAndLimitType(Account account, LimitType limitType);
}
