package com.javaisland.bank_backend.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequestDto {

    @NotBlank(message = "Il nome è obbligatorio")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ' ]+$", message = "Il nome può contenere solo lettere")
    private String firstName;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ' ]+$", message = "Il cognome può contenere solo lettere")
    private String lastName;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Inserisci un indirizzo email valido")
    private String email;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
    private String password;
}
