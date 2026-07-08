package com.javaisland.bank_backend.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CardTypeRepository extends JpaRepository<CardType, Integer> {
    Optional<CardType> findByTypeName(String typeName);
}
