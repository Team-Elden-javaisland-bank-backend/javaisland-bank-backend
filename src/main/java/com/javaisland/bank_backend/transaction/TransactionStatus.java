package com.javaisland.bank_backend.transaction;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "status_name", unique = true, nullable = false, length = 30)
    private String statusName;
}
