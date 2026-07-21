package com.javaisland.bank_backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequestDto {
    private Long id;
    private String type;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
