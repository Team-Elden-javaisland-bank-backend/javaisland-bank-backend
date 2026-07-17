package com.javaisland.bank_backend.user.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatus {

    public static final int PENDING = 1;
    public static final int ACTIVE = 2;
    public static final int ANNULLED = 3;
    public static final int SUSPENDED = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_status", unique = true, nullable = false, length = 30)
    private String userStatus;
}
