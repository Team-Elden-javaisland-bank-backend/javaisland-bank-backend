package com.javaisland.bank_backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistrationDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String email;
    private String profilePictureUrl;
    private OffsetDateTime registeredAt;
}
