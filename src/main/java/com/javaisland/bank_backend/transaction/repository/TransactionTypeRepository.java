package com.javaisland.bank_backend.transaction.repository;

import com.javaisland.bank_backend.transaction.model.TransactionType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TransactionTypeRepository extends JpaRepository<TransactionType, Integer> {

    @Override
    @Cacheable("transactionTypes")
    Optional<TransactionType> findById(Integer id);

    @Cacheable("transactionTypes")
    Optional<TransactionType> findByTypeName(String typeName);
}
