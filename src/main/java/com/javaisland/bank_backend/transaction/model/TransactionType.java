package com.javaisland.bank_backend.transaction.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionType {

    public static final int DEPOSIT = 1;
    public static final int WITHDRAWAL = 2;
    public static final int TRANSFER = 3;
    public static final int INITIAL_TRANSFER = 4;
    public static final int INSTANT_TRANSFER = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_name", unique = true, nullable = false, length = 30)
    private String typeName;
}
