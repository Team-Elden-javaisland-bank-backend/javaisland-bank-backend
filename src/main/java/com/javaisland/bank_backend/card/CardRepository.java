package com.javaisland.bank_backend.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    // Cerca una carta tramite il suo numero a 16 cifre
    Optional<Card> findByCardNumber(String cardNumber);
}