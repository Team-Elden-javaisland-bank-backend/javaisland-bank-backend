package com.javaisland.bank_backend.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaisland.bank_backend.notification.dto.NotificationDto;
import com.javaisland.bank_backend.notification.model.Notification;
import com.javaisland.bank_backend.notification.repository.NotificationRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private MessageSource messageSource;
    @Mock private ObjectMapper objectMapper;
    @Mock private UserRepository userRepository;

    @InjectMocks private NotificationService notificationService;

    @Captor private ArgumentCaptor<Notification> notificationCaptor;

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    private Notification createNotification(Long id, User user, String type, String message, String messageKey, String messageParams, boolean read) {
        return Notification.builder()
                .id(id)
                .user(user)
                .type(type)
                .message(message)
                .messageKey(messageKey)
                .messageParams(messageParams)
                .read(read)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void send_createsNotificationWithAllFields() {
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        notificationService.send(1L, "ACCOUNT", "Test message", "TEST_KEY", "[\"param1\"]");

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals("ACCOUNT", saved.getType());
        assertEquals("Test message", saved.getMessage());
        assertEquals("TEST_KEY", saved.getMessageKey());
        assertEquals("[\"param1\"]", saved.getMessageParams());
    }

    @Test
    void send_createsNotificationWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        notificationService.send(99L, "ACCOUNT", "Orphan message", null, null);

        verify(notificationRepository).save(notificationCaptor.capture());
        assertNull(notificationCaptor.getValue().getUser());
    }

    @Test
    void getNotifications_returnsTranslatedMessages() {
        User user = createUser(1L);
        Notification notif = createNotification(1L, user, "ACCOUNT", "Fallback message", "NOTIF_KEY", null, false);

        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notif));
        when(messageSource.getMessage("NOTIF_KEY", new Object[0], "Fallback message", Locale.ITALY))
                .thenReturn("Messaggio tradotto");

        List<NotificationDto> result = notificationService.getNotifications(1L, Locale.ITALY);

        assertEquals(1, result.size());
        assertEquals("Messaggio tradotto", result.get(0).getMessage());
        assertEquals("NOTIF_KEY", result.get(0).getMessageKey());
    }

    @Test
    void getNotifications_usesFallbackWhenNoKey() {
        User user = createUser(1L);
        Notification notif = createNotification(1L, user, "ACCOUNT", "Direct message", null, null, false);

        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notif));

        List<NotificationDto> result = notificationService.getNotifications(1L, Locale.ITALY);

        assertEquals(1, result.size());
        assertEquals("Direct message", result.get(0).getMessage());
    }

    @Test
    void getNotifications_usesFallbackWhenKeyNotFound() {
        User user = createUser(1L);
        Notification notif = createNotification(1L, user, "ACCOUNT", "Fallback text", "MISSING_KEY", null, false);

        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notif));
        when(messageSource.getMessage("MISSING_KEY", new Object[0], "Fallback text", Locale.ENGLISH))
                .thenReturn("Fallback text");

        List<NotificationDto> result = notificationService.getNotifications(1L, Locale.ENGLISH);

        assertEquals(1, result.size());
        assertEquals("Fallback text", result.get(0).getMessage());
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByUser_IdAndReadFalse(1L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    void markAsRead_updatesNotification() {
        User user = createUser(1L);
        Notification notif = createNotification(1L, user, "ACCOUNT", "msg", null, null, false);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        notificationService.markAsRead(1L, 1L);

        assertTrue(notif.isRead());
        verify(notificationRepository).save(notif);
    }

    @Test
    void markAsRead_ignoresWrongUser() {
        User user = createUser(2L);
        Notification notif = createNotification(1L, user, "ACCOUNT", "msg", null, null, false);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));

        notificationService.markAsRead(1L, 1L);

        assertFalse(notif.isRead());
        verify(notificationRepository, never()).save(notif);
    }

    @Test
    void markAllAsRead_marksOnlyUnreadNotifications() {
        User user = createUser(1L);
        Notification n1 = createNotification(1L, user, "ACCOUNT", "msg1", null, null, true);
        Notification n2 = createNotification(2L, user, "ACCOUNT", "msg2", null, null, false);
        Notification n3 = createNotification(3L, user, "ACCOUNT", "msg3", null, null, false);

        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2, n3));

        notificationService.markAllAsRead(1L);

        assertTrue(n1.isRead());
        assertTrue(n2.isRead());
        assertTrue(n3.isRead());
        verify(notificationRepository, times(2)).save(any());
    }
}
