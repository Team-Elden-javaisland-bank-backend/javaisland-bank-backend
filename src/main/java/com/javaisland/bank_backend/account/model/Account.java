package com.javaisland.bank_backend.account.model;

import com.javaisland.bank_backend.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 50)
    private String accountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "status_id", nullable = false)
    private Integer statusId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closure_requested_at")
    private LocalDateTime closureRequestedAt;

    @Column(name = "closure_rejected_at")
    private LocalDateTime closureRejectedAt;

    @Column(name = "source_account_number", length = 50)
    private String sourceAccountNumber;

    @Column(name = "initial_amount", precision = 15, scale = 2)
    private BigDecimal initialAmount;

    @Version
    private Long version = 0L;
}
