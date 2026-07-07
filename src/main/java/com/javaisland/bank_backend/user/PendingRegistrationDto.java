package com.javaisland.bank_backend.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String pendingAccountNumber;
    private LocalDateTime registeredAt;
}
