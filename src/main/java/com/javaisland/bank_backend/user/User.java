package com.javaisland.bank_backend.user;

import com.javaisland.bank_backend.user.RoleType;
import com.javaisland.bank_backend.user.UserStatus;
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

    @NotBlank(message = "Il Keycloak ID è obbligatorio")
    @Column(name = "keycloak_id", unique = true, nullable = false)
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

    @Size(max = 20, message = "Il codice filiale non può superare i 20 caratteri")
    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_type_id", nullable = false)
    private RoleType roleType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Europe/Rome"));
}