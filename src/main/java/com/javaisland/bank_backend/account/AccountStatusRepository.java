package com.javaisland.bank_backend.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountStatusRepository extends JpaRepository<AccountStatus, Integer> {
    Optional<AccountStatus> findByStatusName(String statusName);
}
