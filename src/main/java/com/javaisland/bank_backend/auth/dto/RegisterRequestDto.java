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

    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ' ]+$", message = "First name can only contain letters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ' ]+$", message = "Last name can only contain letters")
    private String lastName;

    @NotNull(message = "Birth date is required")
    @Adult
    private LocalDate birthDate;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character"
    )
    private String password;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Profession is required")
    private String profession;

    @NotBlank(message = "Fiscal code is required")
    @Size(min = 16, max = 16, message = "Fiscal code must be 16 characters")
    private String fiscalCode;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Pattern(
        regexp = "^\\+39(3\\d{8,9}|0\\d{7,10})$",
        message = "Please enter a valid Italian phone number (e.g., +393331234567)"
    )
    private String phone;

    @NotBlank(message = "Residence is required")
    @Size(max = 200, message = "Residence must not exceed 200 characters")
    private String residence;

    @NotBlank(message = "Birth place is required")
    @Size(max = 100, message = "Birth place must not exceed 100 characters")
    private String birthPlace;

    @NotBlank(message = "Birth province is required")
    @Size(min = 2, max = 2, message = "Birth province must be 2 characters")
    private String birthProvince;
}
