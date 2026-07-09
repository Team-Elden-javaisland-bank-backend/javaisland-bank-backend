package com.javaisland.bank_backend.user.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleType {

    public static final int CUSTOMER = 1;
    public static final int EMPLOYEE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "role_name", unique = true, nullable = false, length = 30)
    private String roleName;
}
