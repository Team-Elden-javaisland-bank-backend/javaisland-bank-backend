package com.javaisland.bank_backend.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", unique = true, nullable = false)
    private String keycloakId;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_name", nullable = false, length = 30)
    private UserStatus status;

    @Column(name = "role_type_id", nullable = false)
    private Integer roleTypeId; // 1=CUSTOMER, 2=EMPLOYEE

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}