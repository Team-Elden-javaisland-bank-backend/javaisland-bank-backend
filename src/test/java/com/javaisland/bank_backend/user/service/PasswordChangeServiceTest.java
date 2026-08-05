package com.javaisland.bank_backend.user.service;

import com.javaisland.bank_backend.auth.service.KeycloakAdminService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.security.EncryptionService;
import com.javaisland.bank_backend.user.dto.PasswordChangeRequestDto;
import com.javaisland.bank_backend.user.dto.PasswordChangeRequestInputDto;
import com.javaisland.bank_backend.user.model.PasswordChangeRequest;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.PasswordChangeRequestRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    @Mock private PasswordChangeRequestRepository passwordChangeRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private KeycloakAdminService keycloakAdminService;
    @Mock private NotificationService notificationService;
    @Mock private UserPinService userPinService;
    @Mock private EncryptionService encryptionService;

    @InjectMocks private PasswordChangeService passwordChangeService;

    private User createUser(Long id, String firstName, String lastName, String email) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setUsername(email);
        user.setKeycloakId("kc-" + id);
        return user;
    }

    private PasswordChangeRequestInputDto createInputDto(String currentPassword, String newPassword, String pin) {
        return PasswordChangeRequestInputDto.builder()
                .currentPassword(currentPassword)
                .newPassword(newPassword)
                .pin(pin)
                .build();
    }

    private PasswordChangeRequest createPendingRequest(Long id, User user) {
        return PasswordChangeRequest.builder()
                .id(id)
                .user(user)
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .newPasswordEncrypted("encrypted")
                .build();
    }

    @Test
    void requestPasswordChange_success() {
        Long userId = 1L;
        User user = createUser(userId, "John", "Doe", "john@test.com");
        PasswordChangeRequestInputDto dto = createInputDto("oldPass1!", "newPass123!", "1234");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordChangeRequestRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, "PENDING"))
                .thenReturn(Optional.empty());
        when(userPinService.verifyPin(userId, "1234")).thenReturn(true);
        when(keycloakAdminService.tokenLogin("john@test.com", "oldPass1!")).thenReturn(Map.of());
        when(encryptionService.encrypt("newPass123!")).thenReturn("encrypted");

        passwordChangeService.requestPasswordChange(userId, dto);

        verify(passwordChangeRequestRepository).save(any(PasswordChangeRequest.class));
        verify(notificationService).send(eq(userId), eq("PASSWORD_CHANGE"), anyString(),
                eq("NOTIF_PWD_CHANGE_REQUESTED"), isNull());
    }

    @Test
    void requestPasswordChange_userNotFound() {
        Long userId = 1L;
        PasswordChangeRequestInputDto dto = createInputDto("oldPass1!", "newPass123!", "1234");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> passwordChangeService.requestPasswordChange(userId, dto));
        verify(passwordChangeRequestRepository, never()).save(any());
    }

    @Test
    void requestPasswordChange_pendingRequestExists() {
        Long userId = 1L;
        User user = createUser(userId, "Jane", "Doe", "jane@test.com");
        PasswordChangeRequest existing = createPendingRequest(10L, user);
        PasswordChangeRequestInputDto dto = createInputDto("oldPass1!", "newPass123!", "1234");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordChangeRequestRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, "PENDING"))
                .thenReturn(Optional.of(existing));

        assertThrows(ApiBankException.class, () -> passwordChangeService.requestPasswordChange(userId, dto));
        verify(passwordChangeRequestRepository, never()).save(any());
        verify(userPinService, never()).verifyPin(anyLong(), any());
        verify(keycloakAdminService, never()).tokenLogin(any(), any());
    }

    @Test
    void requestPasswordChange_invalidPin() {
        Long userId = 1L;
        User user = createUser(userId, "Jane", "Doe", "jane@test.com");
        PasswordChangeRequestInputDto dto = createInputDto("oldPass1!", "newPass123!", "9999");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordChangeRequestRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, "PENDING"))
                .thenReturn(Optional.empty());
        when(userPinService.verifyPin(userId, "9999")).thenReturn(false);

        assertThrows(ApiBankException.class, () -> passwordChangeService.requestPasswordChange(userId, dto));
        verify(keycloakAdminService, never()).tokenLogin(any(), any());
        verify(passwordChangeRequestRepository, never()).save(any());
    }

    @Test
    void requestPasswordChange_wrongCurrentPassword() {
        Long userId = 1L;
        User user = createUser(userId, "Jane", "Doe", "jane@test.com");
        PasswordChangeRequestInputDto dto = createInputDto("wrongPass1!", "newPass123!", "1234");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordChangeRequestRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, "PENDING"))
                .thenReturn(Optional.empty());
        when(userPinService.verifyPin(userId, "1234")).thenReturn(true);
        when(keycloakAdminService.tokenLogin("jane@test.com", "wrongPass1!"))
                .thenThrow(new ApiBankException("INVALID_CREDENTIALS", "INVALID_CREDENTIALS"));

        ApiBankException ex = assertThrows(ApiBankException.class,
                () -> passwordChangeService.requestPasswordChange(userId, dto));
        assertEquals("CURRENT_PASSWORD_INCORRECT", ex.getErrorCode());
        verify(passwordChangeRequestRepository, never()).save(any());
    }

    @Test
    void requestPasswordChange_newPasswordSameAsCurrent() {
        Long userId = 1L;
        User user = createUser(userId, "Jane", "Doe", "jane@test.com");
        PasswordChangeRequestInputDto dto = createInputDto("samePass1!", "samePass1!", "1234");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordChangeRequestRepository.findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, "PENDING"))
                .thenReturn(Optional.empty());
        when(userPinService.verifyPin(userId, "1234")).thenReturn(true);
        when(keycloakAdminService.tokenLogin("jane@test.com", "samePass1!")).thenReturn(Map.of());

        ApiBankException ex = assertThrows(ApiBankException.class,
                () -> passwordChangeService.requestPasswordChange(userId, dto));
        assertEquals("NEW_PASSWORD_SAME_AS_CURRENT", ex.getErrorCode());
        verify(encryptionService, never()).encrypt(any());
        verify(passwordChangeRequestRepository, never()).save(any());
    }

    @Test
    void getPendingRequests_returnsList() {
        User user = createUser(1L, "Alice", "Smith", "alice@test.com");
        PasswordChangeRequest req1 = createPendingRequest(1L, user);
        PasswordChangeRequest req2 = createPendingRequest(2L, user);

        when(passwordChangeRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING"))
                .thenReturn(List.of(req1, req2));

        List<PasswordChangeRequestDto> result = passwordChangeService.getPendingRequests();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Alice", result.get(0).getUserFirstName());
        assertEquals("Smith", result.get(0).getUserLastName());
        assertEquals("alice@test.com", result.get(0).getUserEmail());
    }

    @Test
    void approveRequest_success() {
        Long requestId = 1L;
        User user = createUser(1L, "Bob", "Brown", "bob@test.com");
        PasswordChangeRequest request = createPendingRequest(requestId, user);

        when(passwordChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(encryptionService.decrypt("encrypted")).thenReturn("newPass123!");

        passwordChangeService.approveRequest(requestId);

        assertEquals("APPROVED", request.getStatus());
        assertNotNull(request.getProcessedAt());
        assertNull(request.getNewPasswordEncrypted());
        verify(userRepository).save(user);
        verify(passwordChangeRequestRepository).save(request);
        verify(keycloakAdminService).resetPassword("kc-1", "newPass123!");
        verify(keycloakAdminService).logoutUser("kc-1");
        verify(notificationService).send(eq(1L), eq("PASSWORD_CHANGE"), anyString(),
                eq("NOTIF_PWD_CHANGE_APPROVED"), isNull());
    }

    @Test
    void approveRequest_missingEncryptedPassword() {
        Long requestId = 1L;
        User user = createUser(1L, "Bob", "Brown", "bob@test.com");
        PasswordChangeRequest request = PasswordChangeRequest.builder()
                .id(requestId)
                .user(user)
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .build();

        when(passwordChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ApiBankException.class, () -> passwordChangeService.approveRequest(requestId));
        verify(keycloakAdminService, never()).resetPassword(any(), any());
        verify(passwordChangeRequestRepository, never()).save(request);
    }

    @Test
    void approveRequest_requestNotFound() {
        Long requestId = 1L;

        when(passwordChangeRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> passwordChangeService.approveRequest(requestId));
        verify(passwordChangeRequestRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void approveRequest_requestNotPending() {
        Long requestId = 1L;
        User user = createUser(1L, "Carol", "White", "carol@test.com");
        PasswordChangeRequest request = PasswordChangeRequest.builder()
                .id(requestId)
                .user(user)
                .status("APPROVED")
                .createdAt(OffsetDateTime.now())
                .build();

        when(passwordChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ApiBankException.class, () -> passwordChangeService.approveRequest(requestId));
        verify(passwordChangeRequestRepository, never()).save(request);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectRequest_success() {
        Long requestId = 1L;
        User user = createUser(1L, "Dave", "Green", "dave@test.com");
        PasswordChangeRequest request = createPendingRequest(requestId, user);

        when(passwordChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        passwordChangeService.rejectRequest(requestId);

        assertEquals("REJECTED", request.getStatus());
        assertNotNull(request.getProcessedAt());
        verify(passwordChangeRequestRepository).save(request);
        verify(notificationService).send(eq(1L), eq("PASSWORD_CHANGE"), anyString(),
                eq("NOTIF_PWD_CHANGE_REJECTED"), isNull());
    }

    @Test
    void rejectRequest_requestNotFound() {
        Long requestId = 1L;

        when(passwordChangeRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class, () -> passwordChangeService.rejectRequest(requestId));
        verify(passwordChangeRequestRepository, never()).save(any());
    }

    @Test
    void rejectRequest_requestNotPending() {
        Long requestId = 1L;
        User user = createUser(1L, "Eve", "Black", "eve@test.com");
        PasswordChangeRequest request = PasswordChangeRequest.builder()
                .id(requestId)
                .user(user)
                .status("REJECTED")
                .createdAt(OffsetDateTime.now())
                .build();

        when(passwordChangeRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(ApiBankException.class, () -> passwordChangeService.rejectRequest(requestId));
        verify(passwordChangeRequestRepository, never()).save(request);
    }
}
