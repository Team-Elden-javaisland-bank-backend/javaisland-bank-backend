package com.javaisland.bank_backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;
    private Integer statusId;
    private String profession;
    private String gender;
    private String fiscalCode;
    private String phone;
    private String residence;
    private String birthPlace;
    private String birthProvince;
}
