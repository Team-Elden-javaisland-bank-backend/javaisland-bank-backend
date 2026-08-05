package com.javaisland.bank_backend.card.repository;

import com.javaisland.bank_backend.card.model.CardStatus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CardStatusRepository extends JpaRepository<CardStatus, Integer> {

    @Override
    @Cacheable("cardStatuses")
    Optional<CardStatus> findById(Integer id);

    @Cacheable("cardStatuses")
    Optional<CardStatus> findByStatusName(String statusName);
}
