package com.javaisland.bank_backend.transaction.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatus {

    public static final int PENDING = 1;
    public static final int COMPLETED = 2;
    public static final int FAILED = 3;
    public static final int REJECTED = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "status_name", unique = true, nullable = false, length = 30)
    private String statusName;
}
