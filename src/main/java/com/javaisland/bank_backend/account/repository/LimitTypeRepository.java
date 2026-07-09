package com.javaisland.bank_backend.account.repository;

import com.javaisland.bank_backend.account.model.LimitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LimitTypeRepository extends JpaRepository<LimitType, Integer> {
    Optional<LimitType> findByLimitName(String limitName);
}
