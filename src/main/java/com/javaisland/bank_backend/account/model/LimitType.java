package com.javaisland.bank_backend.account.model;

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

    @Column(name = "change_policy", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ChangePolicy changePolicy;

    public enum ChangePolicy {
        USER_FULL,
        USER_LOWER_ONLY,
        BANK_ONLY
    }
}
