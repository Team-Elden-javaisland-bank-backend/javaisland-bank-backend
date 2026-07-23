package com.javaisland.bank_backend.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaisland.bank_backend.notification.dto.NotificationDto;
import com.javaisland.bank_backend.notification.model.Notification;
import com.javaisland.bank_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    public void send(Long userId, String type, String message, String messageKey, String messageParams) {
        Notification n = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .messageKey(messageKey)
                .messageParams(messageParams)
                .build();
        notificationRepository.save(n);
        log.info("Notification sent to user id={}: [{}] {} (key={})", userId, type, message, messageKey);
    }

    public List<NotificationDto> getNotifications(Long userId, Locale locale) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> {
                    String translatedMessage = n.getMessage();
                    if (n.getMessageKey() != null && !n.getMessageKey().isBlank()) {
                        Object[] args = parseParams(n.getMessageParams());
                        translatedMessage = messageSource.getMessage(n.getMessageKey(), args, n.getMessage(), locale);
                    }
                    return NotificationDto.builder()
                            .id(n.getId())
                            .type(n.getType())
                            .message(translatedMessage)
                            .messageKey(n.getMessageKey())
                            .read(n.isRead())
                            .createdAt(n.getCreatedAt())
                            .build();
                })
                .toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !n.isRead())
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    private Object[] parseParams(String messageParams) {
        if (messageParams == null || messageParams.isBlank()) {
            return new Object[0];
        }
        try {
            String[] raw = objectMapper.readValue(messageParams, String[].class);
            Object[] result = new Object[raw.length];
            System.arraycopy(raw, 0, result, 0, raw.length);
            return result;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse notification params: {}", messageParams);
            return new Object[0];
        }
    }
}
