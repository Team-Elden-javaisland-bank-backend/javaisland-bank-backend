package com.javaisland.bank_backend.card.repository;

import com.javaisland.bank_backend.card.model.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CardTypeRepository extends JpaRepository<CardType, Integer> {
    Optional<CardType> findByTypeName(String typeName);
}
