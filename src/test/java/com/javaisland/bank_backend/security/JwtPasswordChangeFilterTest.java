package com.javaisland.bank_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.model.UserStatus;
import com.javaisland.bank_backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtPasswordChangeFilterTest {

    @Mock private UserRepository userRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtPasswordChangeFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setStaleTokenAuth(String keycloakId) {
        Jwt jwt = Jwt.withTokenValue("old-token")
                .header("alg", "none")
                .subject(keycloakId)
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private User activeUserWithRecentPasswordChange() {
        User user = new User();
        user.setId(1L);
        user.setStatus(new UserStatus(2, "ACTIVE"));
        user.setPasswordChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return user;
    }

    private void mockWriter() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
    }

    @Test
    void businessRequest_withStaleToken_isRejectedWith401() throws Exception {
        setStaleTokenAuth("kc-1");
        when(request.getRequestURI()).thenReturn("/api/v1/account/list");
        when(userRepository.findByKeycloakIdWithStatus("kc-1"))
                .thenReturn(Optional.of(activeUserWithRecentPasswordChange()));
        mockWriter();
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        filter.doFilter(request, response, filterChain);

        ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(response).setStatus(statusCaptor.capture());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, statusCaptor.getValue());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void loginRequest_withStaleToken_passesThrough() throws Exception {
        setStaleTokenAuth("kc-1");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/keycloak-login");

        filter.doFilter(request, response, filterChain);

        verify(response, never()).setStatus(any(Integer.class));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void logoutRequest_withStaleToken_passesThrough() throws Exception {
        setStaleTokenAuth("kc-1");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/logout");

        filter.doFilter(request, response, filterChain);

        verify(response, never()).setStatus(any(Integer.class));
        verify(filterChain).doFilter(request, response);
    }
}
