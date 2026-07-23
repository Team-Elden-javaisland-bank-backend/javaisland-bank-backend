package com.javaisland.bank_backend.notification.controller;

import com.javaisland.bank_backend.notification.dto.NotificationDto;
import com.javaisland.bank_backend.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.javaisland.bank_backend.user.repository.UserRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customer/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        Long userId = getUserId(jwt);
        Locale locale = resolveLocale(request);
        return ResponseEntity.ok(notificationService.getNotifications(userId, locale));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserId(jwt);
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        notificationService.markAsRead(id, getUserId(jwt));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(getUserId(jwt));
        return ResponseEntity.ok().build();
    }

    private Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            try {
                return Locale.forLanguageTag(acceptLanguage.split(",")[0].trim());
            } catch (Exception e) {
                // fallback to Italian
            }
        }
        return Locale.ITALIAN;
    }

    private Long getUserId(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return userRepository.findByKeycloakId(keycloakId)
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
