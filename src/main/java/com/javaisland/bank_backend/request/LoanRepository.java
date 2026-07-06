package com.javaisland.bank_backend.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Recupera tutti i finanziamenti di un determinato cliente
    List<Loan> findByUserId(Long userId);
}