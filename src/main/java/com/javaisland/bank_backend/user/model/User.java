package com.javaisland.bank_backend.user.model;

import com.javaisland.bank_backend.user.model.RoleType;
import com.javaisland.bank_backend.user.model.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @NotBlank(message = "Lo username non può essere vuoto")
    @Size(min = 4, max = 50, message = "Lo username deve avere tra 4 e 50 caratteri")
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(max = 100, message = "Il nome non può superare i 100 caratteri")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(max = 100, message = "Il cognome non può superare i 100 caratteri")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotNull(message = "La data di nascita è obbligatoria")
    @Past(message = "La data di nascita deve essere una data passata")
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Inserisci un indirizzo email formattato correttamente")
    @Size(max = 150, message = "L'email non può superare i 150 caratteri")
    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @NotBlank(message = "La password è obbligatoria")
    @Column(nullable = false, length = 64)
    private String password;

    @Column(name = "plain_password", length = 100)
    private String plainPassword;

    @Size(max = 20, message = "Il codice filiale non può superare i 20 caratteri")
    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @NotBlank(message = "La professione è obbligatoria")
    @Size(max = 100, message = "La professione non può superare i 100 caratteri")
    @Column(name = "profession", length = 100)
    private String profession;

    @NotBlank(message = "Il sesso è obbligatorio")
    @Size(min = 1, max = 1, message = "Il sesso deve essere M o F")
    @Column(name = "gender", length = 1)
    private String gender;

    @NotBlank(message = "Il codice fiscale è obbligatorio")
    @Size(min = 16, max = 16, message = "Il codice fiscale deve essere di 16 caratteri")
    @Column(name = "fiscal_code", unique = true, length = 16)
    private String fiscalCode;

    @NotBlank(message = "Il telefono è obbligatorio")
    @Size(max = 20, message = "Il telefono non può superare i 20 caratteri")
    @Column(name = "phone", length = 20)
    private String phone;

    @NotBlank(message = "La residenza è obbligatoria")
    @Size(max = 200, message = "La residenza non può superare i 200 caratteri")
    @Column(name = "residence", length = 200)
    private String residence;

    @NotBlank(message = "Il luogo di nascita è obbligatorio")
    @Size(max = 100, message = "Il luogo di nascita non può superare i 100 caratteri")
    @Column(name = "birth_place", length = 100)
    private String birthPlace;

    @NotBlank(message = "La provincia di nascita è obbligatoria")
    @Size(min = 2, max = 2, message = "La provincia deve essere di 2 caratteri")
    @Column(name = "birth_province", length = 2)
    private String birthProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_type_id", nullable = false)
    private RoleType roleType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Europe/Rome"));

    @Column(name = "limits_setup_complete", nullable = false)
    private boolean limitsSetupComplete = false;
}