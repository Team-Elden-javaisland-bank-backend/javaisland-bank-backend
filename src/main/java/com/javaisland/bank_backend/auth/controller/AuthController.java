package com.javaisland.bank_backend.auth.controller;

import com.javaisland.bank_backend.auth.dto.LoginRequestDto;
import com.javaisland.bank_backend.auth.dto.LoginResponseDto;
import com.javaisland.bank_backend.auth.dto.RegisterRequestDto;
import com.javaisland.bank_backend.auth.service.AuthService;
import com.javaisland.bank_backend.auth.service.RegistrationService;
import com.javaisland.bank_backend.user.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Registration and login endpoints")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthService authService;

    @Value("${app.jwt.cookie-name:bank_token}")
    private String cookieName;

    @Value("${app.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer", description = "Creates a new pending customer registration in both bank DB and Keycloak")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registration created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or duplicate email/username")
    })
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto requestDto) {
        return ResponseEntity.ok(registrationService.register(requestDto));
    }

    @PostMapping("/keycloak-login")
    @Operation(summary = "Login with Keycloak", description = "Authenticates user via Keycloak and syncs to local DB")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<LoginResponseDto> keycloakLogin(@Valid @RequestBody LoginRequestDto request,
                                                          HttpServletResponse response) {
        LoginResponseDto dto = authService.keycloakLogin(request);
        if (dto.getToken() != null) {
            setAuthCookie(response, dto.getToken());
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Returns the authenticated user profile (session restore)")
    public ResponseEntity<LoginResponseDto> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getUserInfo(jwt));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearAuthCookie(response);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(12))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
