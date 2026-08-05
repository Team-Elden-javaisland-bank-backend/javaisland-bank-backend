package com.javaisland.bank_backend.admin.dto;

import com.javaisland.bank_backend.account.dto.AccountResponseDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class AdminCustomerDetailDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private LocalDate birthDate;
    private String gender;
    private String fiscalCode;
    private String phone;
    private String residence;
    private String birthPlace;
    private String birthProvince;
    private String profession;
    private String status;
    private String profilePictureUrl;
    private OffsetDateTime createdAt;
    private List<AccountResponseDto> accounts;
}
