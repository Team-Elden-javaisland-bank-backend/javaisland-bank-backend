package com.javaisland.bank_backend.transaction;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transaction_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_name", unique = true, nullable = false, length = 30)
    private String typeName;
}
