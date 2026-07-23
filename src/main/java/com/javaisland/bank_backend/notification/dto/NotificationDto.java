package com.javaisland.bank_backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDto {
    private Long id;
    private String type;
    private String message;
    private String messageKey;
    private String messageParams;
    private boolean read;
    private LocalDateTime createdAt;
}
