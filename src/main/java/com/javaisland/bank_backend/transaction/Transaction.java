package com.javaisland.bank_backend.transaction;

import com.javaisland.bank_backend.account.Account;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "type_id", nullable = false)
    private Integer typeId; // see TransactionType

    @Column(name = "status_id", nullable = false)
    private Integer statusId; // see TransactionStatus

    private String description;

    @Column(name = "source_balance_after", precision = 15, scale = 2)
    private BigDecimal sourceBalanceAfter;

    @Column(name = "dest_balance_after", precision = 15, scale = 2)
    private BigDecimal destBalanceAfter;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;
}
