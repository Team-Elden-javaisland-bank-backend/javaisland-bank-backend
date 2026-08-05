package com.javaisland.bank_backend.user.model;

import com.javaisland.bank_backend.user.model.RoleType;
import com.javaisland.bank_backend.user.model.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    @NotBlank(message = "Username must not be empty")
    @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters")
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Past(message = "Birth date must be in the past")
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a properly formatted email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Size(max = 20, message = "Branch code must not exceed 20 characters")
    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @NotBlank(message = "Profession is required")
    @Size(max = 100, message = "Profession must not exceed 100 characters")
    @Column(name = "profession", length = 100)
    private String profession;

    @NotBlank(message = "Gender is required")
    @Size(min = 1, max = 1, message = "Gender must be M or F")
    @Column(name = "gender", length = 1)
    private String gender;

    @NotBlank(message = "Fiscal code is required")
    @Size(min = 16, max = 16, message = "Fiscal code must be 16 characters")
    @Column(name = "fiscal_code", unique = true, length = 16)
    private String fiscalCode;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Column(name = "phone", length = 20)
    private String phone;

    @NotBlank(message = "Residence is required")
    @Size(max = 200, message = "Residence must not exceed 200 characters")
    @Column(name = "residence", length = 200)
    private String residence;

    @NotBlank(message = "Birth place is required")
    @Size(max = 100, message = "Birth place must not exceed 100 characters")
    @Column(name = "birth_place", length = 100)
    private String birthPlace;

    @NotBlank(message = "Birth province is required")
    @Size(min = 2, max = 2, message = "Birth province must be 2 characters")
    @Column(name = "birth_province", length = 2)
    private String birthProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_type_id", nullable = false)
    private RoleType roleType;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneId.of("Europe/Rome"));

    @Column(name = "limits_setup_complete", nullable = false)
    private boolean limitsSetupComplete = false;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "password_changed_at")
    private OffsetDateTime passwordChangedAt;
}