package com.javaisland.bank_backend.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BankRequestRepository extends JpaRepository<BankRequest, Long> {

    // Recupera tutte le richieste fatte da un singolo utente
    List<BankRequest> findByUserId(Long userId);
}