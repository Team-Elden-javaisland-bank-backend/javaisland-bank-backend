package com.javaisland.bank_backend.auth.dto;

import com.javaisland.bank_backend.validation.Adult;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "Il nome è obbligatorio")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ' ]+$", message = "Il nome può contenere solo lettere")
    private String firstName;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ' ]+$", message = "Il cognome può contenere solo lettere")
    private String lastName;

    @NotNull(message = "La data di nascita è obbligatoria")
    @Adult
    private LocalDate birthDate;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Inserisci un indirizzo email valido")
    private String email;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
        message = "La password deve contenere almeno una lettera maiuscola, una minuscola, un numero e un carattere speciale"
    )
    private String password;

    @NotBlank(message = "Il sesso è obbligatorio")
    private String gender;

    @NotBlank(message = "La professione è obbligatoria")
    private String profession;

    @NotBlank(message = "Il codice fiscale è obbligatorio")
    @Size(min = 16, max = 16, message = "Il codice fiscale deve essere di 16 caratteri")
    private String fiscalCode;

    @NotBlank(message = "Il telefono è obbligatorio")
    @Size(max = 20, message = "Il telefono non può superare i 20 caratteri")
    private String phone;

    @NotBlank(message = "La residenza è obbligatoria")
    @Size(max = 200, message = "La residenza non può superare i 200 caratteri")
    private String residence;

    @NotBlank(message = "Il luogo di nascita è obbligatorio")
    @Size(max = 100, message = "Il luogo di nascita non può superare i 100 caratteri")
    private String birthPlace;

    @NotBlank(message = "La provincia di nascita è obbligatoria")
    @Size(min = 2, max = 2, message = "La provincia deve essere di 2 caratteri")
    private String birthProvince;
}
