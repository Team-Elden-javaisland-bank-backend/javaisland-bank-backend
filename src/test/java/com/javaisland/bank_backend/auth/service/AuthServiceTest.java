package com.javaisland.bank_backend.auth.service;

import com.javaisland.bank_backend.auth.dto.LoginRequestDto;
import com.javaisland.bank_backend.auth.dto.LoginResponseDto;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.RoleType;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.model.UserStatus;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import com.javaisland.bank_backend.user.service.UserPinService;
import com.nimbusds.jose.Payload;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserPinService userPinService;
    @Mock private KeycloakAdminService keycloakAdminService;
    @Mock private RoleTypeRepository roleTypeRepository;
    @Mock private UserStatusRepository userStatusRepository;

    @InjectMocks private AuthService authService;

    @Test
    void login_activeUser_returnsToken() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("test@test.com");
        request.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setUsername("test@test.com");
        user.setEmail("test@test.com");
        user.setFirstName("Test");
        user.setLastName("User");
        RoleType role = new RoleType();
        role.setRoleName("C");
        user.setRoleType(role);
        UserStatus status = new UserStatus();
        status.setUserStatus("ACTIVE");
        user.setStatus(status);
        user.setKeycloakId("kc-id");
        user.setLimitsSetupComplete(true);

        when(userRepository.findByUsername("test@test.com")).thenReturn(Optional.of(user));
        when(keycloakAdminService.tokenLogin("test@test.com", "password"))
                .thenReturn(Map.of("access_token", "jwt-token"));
        when(userPinService.hasPin(1L)).thenReturn(true);

        try (MockedStatic<SignedJWT> mockedJwt = mockStatic(SignedJWT.class)) {
            SignedJWT mockParsed = mock(SignedJWT.class);
            when(mockParsed.getJWTClaimsSet()).thenReturn(new com.nimbusds.jwt.JWTClaimsSet.Builder().subject("kc-id").build());
            mockedJwt.when(() -> SignedJWT.parse("jwt-token")).thenReturn(mockParsed);

            LoginResponseDto response = authService.keycloakLogin(request);

            assertNotNull(response);
            assertEquals("C", response.getRole());
            assertEquals(1L, response.getUserId());
            assertTrue(response.isPinSetupComplete());
        }
    }

    @Test
    void login_pendingUser_throwsException() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("pending@test.com");
        request.setPassword("password");

        User user = new User();
        user.setId(2L);
        user.setUsername("pending@test.com");
        UserStatus status = new UserStatus();
        status.setUserStatus("PENDING");
        user.setStatus(status);

        when(userRepository.findByUsername("pending@test.com")).thenReturn(Optional.of(user));

        assertThrows(ApiBankException.class, () -> authService.keycloakLogin(request));
        verify(keycloakAdminService, never()).tokenLogin(any(), any());
    }

    @Test
    void login_suspendedUser_throwsException() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("suspended@test.com");
        request.setPassword("password");

        User user = new User();
        user.setUsername("suspended@test.com");
        UserStatus status = new UserStatus();
        status.setUserStatus("SUSPENDED");
        user.setStatus(status);

        when(userRepository.findByUsername("suspended@test.com")).thenReturn(Optional.of(user));

        assertThrows(ApiBankException.class, () -> authService.keycloakLogin(request));
    }

    @Test
    void login_annulledUser_throwsException() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("annulled@test.com");
        request.setPassword("password");

        User user = new User();
        user.setUsername("annulled@test.com");
        UserStatus status = new UserStatus();
        status.setUserStatus("ANNULLED");
        user.setStatus(status);

        when(userRepository.findByUsername("annulled@test.com")).thenReturn(Optional.of(user));

        assertThrows(ApiBankException.class, () -> authService.keycloakLogin(request));
    }

    @Test
    void login_invalidCredentials_throwsException() {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("test@test.com");
        request.setPassword("wrong");

        when(userRepository.findByUsername("test@test.com")).thenReturn(Optional.empty());
        when(keycloakAdminService.tokenLogin("test@test.com", "wrong"))
                .thenThrow(new ApiBankException("INVALID_CREDENTIALS", "INVALID_CREDENTIALS"));

        assertThrows(ApiBankException.class, () -> authService.keycloakLogin(request));
    }

    @Test
    void login_newUser_syncsAsPendingAndBlocksLogin() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setUsername("new@test.com");
        request.setPassword("password");

        when(userRepository.findByUsername("new@test.com")).thenReturn(Optional.empty());
        when(keycloakAdminService.tokenLogin("new@test.com", "password"))
                .thenReturn(Map.of("access_token", "jwt-token"));

        when(keycloakAdminService.getAllUsers()).thenReturn(java.util.List.of(
                Map.of("id", "new-kc-id", "username", "new@test.com", "firstName", "New", "lastName", "User", "email", "new@test.com")
        ));

        RoleType role = new RoleType();
        role.setRoleName("C");
        when(roleTypeRepository.findByRoleName("C")).thenReturn(Optional.of(role));

        UserStatus pendingStatus = new UserStatus();
        pendingStatus.setUserStatus("PENDING");
        when(userStatusRepository.findByUserStatus("PENDING")).thenReturn(Optional.of(pendingStatus));

        when(userRepository.save(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        try (MockedStatic<SignedJWT> mockedJwt = mockStatic(SignedJWT.class)) {
            SignedJWT mockParsed = mock(SignedJWT.class);
            when(mockParsed.getJWTClaimsSet()).thenReturn(new com.nimbusds.jwt.JWTClaimsSet.Builder().subject("new-kc-id").build());
            mockedJwt.when(() -> SignedJWT.parse("jwt-token")).thenReturn(mockParsed);

            ApiBankException ex = assertThrows(ApiBankException.class, () -> authService.keycloakLogin(request));

            assertEquals("ACCOUNT_PENDING", ex.getErrorCode());
            verify(userRepository, atLeastOnce()).save(any());
        }
    }
}
