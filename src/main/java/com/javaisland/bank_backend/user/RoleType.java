package com.javaisland.bank_backend.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "role_name", unique = true, nullable = false, length = 30)
    private String roleName;
}
