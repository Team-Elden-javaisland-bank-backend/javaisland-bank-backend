package com.javaisland.bank_backend.request;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // 'PENDING', 'APPROVED', 'REJECTED'

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Utente che fa la richiesta

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId; // Impiegato che approva/rifiuta

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}