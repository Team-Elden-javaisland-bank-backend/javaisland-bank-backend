package com.javaisland.bank_backend.card.repository;

import com.javaisland.bank_backend.card.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    boolean existsByCardNumberHash(String cardNumberHash);

    List<Card> findByAccountId(Long accountId);

    List<Card> findByAccountIdIn(List<Long> accountIds);
}