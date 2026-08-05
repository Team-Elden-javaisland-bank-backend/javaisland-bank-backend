package com.javaisland.bank_backend.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaisland.bank_backend.notification.dto.NotificationDto;
import com.javaisland.bank_backend.notification.model.Notification;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.notification.repository.NotificationRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    public void send(Long userId, String type, String message, String messageKey, String messageParams) {
        if (userId == null) {
            log.warn("Skipping notification: user id is null (type={})", type);
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .messageKey(messageKey)
                .messageParams(messageParams)
                .build();
        notificationRepository.save(n);
        log.info("Notification sent to user id={}: [{}] {} (key={})", userId, type, message, messageKey);
    }

    public List<NotificationDto> getNotifications(Long userId, Locale locale) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(n -> {
                    String translatedMessage = n.getMessage();
                    if (n.getMessageKey() != null && !n.getMessageKey().isBlank()) {
                        Object[] args = translateLimitParams(parseParams(n.getMessageParams()), locale);
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
        return notificationRepository.countByUser_IdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
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

    private Object[] translateLimitParams(Object[] args, Locale locale) {
        if (args.length == 0) {
            return args;
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }
            String translated = messageSource.getMessage(
                    "LIMIT_TYPE." + args[i] + ".label", null, null, locale);
            if (translated != null) {
                args[i] = translated;
            }
        }
        return args;
    }
}
