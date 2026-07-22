package com.javaisland.bank_backend.account.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "limit_change_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LimitChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "limit_type_name", nullable = false, length = 30)
    private String limitTypeName;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "current_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Europe/Rome"));
        }
    }
}
