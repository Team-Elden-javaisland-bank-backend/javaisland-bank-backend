package com.javaisland.bank_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtPasswordChangeFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            String keycloakId = jwtAuth.getToken().getSubject();
            Instant tokenIssuedAt = jwtAuth.getToken().getIssuedAt();

            if (keycloakId != null && tokenIssuedAt != null) {
                Optional<User> userOpt = userRepository.findByKeycloakIdWithStatus(keycloakId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    String statusName = user.getStatus() != null ? user.getStatus().getUserStatus() : null;
                    if (statusName != null && !"ACTIVE".equals(statusName)) {
                        log.warn("Rejected request for suspended/annulled user id={}, status={}", user.getId(), statusName);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(objectMapper.writeValueAsString(
                                Map.of("message", "Account non attivo. Contatta il supporto.", "code", "ACCOUNT_" + statusName)));
                        return;
                    }

                    if (user.getPasswordChangedAt() != null
                            && tokenIssuedAt.isBefore(user.getPasswordChangedAt().atZone(ZoneId.of("Europe/Rome")).toInstant())) {
                        log.warn("Rejected token for user id={}: token issued at {} but password changed at {}",
                                user.getId(), tokenIssuedAt, user.getPasswordChangedAt());
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(objectMapper.writeValueAsString(
                                Map.of("message", "Sessione scaduta. Effettua nuovamente il login.", "code", "TOKEN_REVOKED")));
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
