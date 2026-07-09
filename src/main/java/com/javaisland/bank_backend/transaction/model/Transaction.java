package com.javaisland.bank_backend.transaction.model;

import com.javaisland.bank_backend.account.model.Account;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "type_id", nullable = false)
    private Integer typeId;

    @Column(name = "status_id", nullable = false)
    private Integer statusId;

    private String description;

    @Column(name = "source_balance_after", precision = 15, scale = 2)
    private BigDecimal sourceBalanceAfter;

    @Column(name = "dest_balance_after", precision = 15, scale = 2)
    private BigDecimal destBalanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;
}
