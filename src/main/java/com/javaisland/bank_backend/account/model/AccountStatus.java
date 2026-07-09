package com.javaisland.bank_backend.account.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "account_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatus {

    public static final int INACTIVE = 1;
    public static final int ACTIVE = 2;
    public static final int FROZEN = 3;
    public static final int CLOSED = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "status_name", unique = true, nullable = false, length = 30)
    private String statusName;
}
