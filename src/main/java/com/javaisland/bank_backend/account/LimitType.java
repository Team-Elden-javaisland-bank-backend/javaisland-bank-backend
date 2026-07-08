package com.javaisland.bank_backend.account;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "limit_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LimitType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "limit_name", unique = true, nullable = false, length = 30)
    private String limitName;

    @Column(length = 255)
    private String notes;
}
