package com.javaisland.bank_backend.card.repository;

import com.javaisland.bank_backend.card.model.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CardStatusRepository extends JpaRepository<CardStatus, Integer> {
    Optional<CardStatus> findByStatusName(String statusName);
}
