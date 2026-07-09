package com.javaisland.bank_backend.card.repository;

import com.javaisland.bank_backend.card.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByCardNumber(String cardNumber);

    List<Card> findByAccountId(Long accountId);

    List<Card> findByAccountIdIn(List<Long> accountIds);
}