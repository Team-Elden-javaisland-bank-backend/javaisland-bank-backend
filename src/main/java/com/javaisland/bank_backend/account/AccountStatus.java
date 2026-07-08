package com.javaisland.bank_backend.account;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "status_name", unique = true, nullable = false, length = 30)
    private String statusName;
}
