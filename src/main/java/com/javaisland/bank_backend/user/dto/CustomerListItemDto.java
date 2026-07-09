package com.javaisland.bank_backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerListItemDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private Integer statusId;
}
