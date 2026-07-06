package com.javaisland.bank_backend.request;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanAmount; // Importo totale richiesto

    @Column(name = "monthly_installment", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyInstallment; // Rata mensile calcolata

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate; // Tasso d'interesse (es. 5.50)

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths; // Durata in mesi

    @Column(name = "status", nullable = false, length = 30)
    private String status; // 'PENDING', 'APPROVED', 'REJECTED'

    @Column(name = "user_id", nullable = false)
    private Long userId; // Cliente richiedente

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}