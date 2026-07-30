package com.javaisland.bank_backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDto {
    private Long id;
    private String type;
    private String status;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime processedAt;
}
