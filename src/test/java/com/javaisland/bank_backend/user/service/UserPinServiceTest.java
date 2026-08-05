package com.javaisland.bank_backend.user.service;

import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.model.UserPin;
import com.javaisland.bank_backend.user.repository.UserPinRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPinServiceTest {

    @Mock private UserPinRepository userPinRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserPinService userPinService;

    @Captor private ArgumentCaptor<UserPin> userPinCaptor;

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    @Test
    void setupPin_createsPinSuccessfully() {
        User user = createUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userPinRepository.existsByUserId(1L)).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("$2a$10$hashedPin");

        userPinService.setupPin(1L, "1234");

        verify(userPinRepository).save(userPinCaptor.capture());
        UserPin saved = userPinCaptor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals("$2a$10$hashedPin", saved.getPinHash());
    }

    @Test
    void setupPin_throwsWhenPinAlreadyExists() {
        when(userPinRepository.existsByUserId(1L)).thenReturn(true);

        ApiBankException ex = assertThrows(ApiBankException.class,
                () -> userPinService.setupPin(1L, "1234"));
        assertEquals("PIN_ALREADY_EXISTS", ex.getMessage());
        verify(userRepository, never()).findById(any());
        verify(userPinRepository, never()).save(any());
    }

    @Test
    void setupPin_throwsWhenInvalidFormat() {
        ApiBankException ex = assertThrows(ApiBankException.class,
                () -> userPinService.setupPin(1L, "123"));
        assertEquals("INVALID_PIN_FORMAT", ex.getMessage());

        ApiBankException ex2 = assertThrows(ApiBankException.class,
                () -> userPinService.setupPin(1L, "abcde"));
        assertEquals("INVALID_PIN_FORMAT", ex2.getMessage());

        ApiBankException ex3 = assertThrows(ApiBankException.class,
                () -> userPinService.setupPin(1L, null));
        assertEquals("INVALID_PIN_FORMAT", ex3.getMessage());
    }

    @Test
    void setupPin_throwsWhenUserNotFound() {
        when(userPinRepository.existsByUserId(99L)).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ApiBankException.class,
                () -> userPinService.setupPin(99L, "1234"));
    }

    @Test
    void hasPin_returnsTrueWhenExists() {
        when(userPinRepository.existsByUserId(1L)).thenReturn(true);
        assertTrue(userPinService.hasPin(1L));
    }

    @Test
    void hasPin_returnsFalseWhenNotExists() {
        when(userPinRepository.existsByUserId(1L)).thenReturn(false);
        assertFalse(userPinService.hasPin(1L));
    }

    @Test
    void verifyPin_returnsTrueOnMatch() {
        UserPin userPin = new UserPin();
        userPin.setPinHash("$2a$10$hashedPin");
        when(userPinRepository.findByUserId(1L)).thenReturn(Optional.of(userPin));
        when(passwordEncoder.matches("1234", "$2a$10$hashedPin")).thenReturn(true);

        assertTrue(userPinService.verifyPin(1L, "1234"));
    }

    @Test
    void verifyPin_returnsFalseOnMismatch() {
        UserPin userPin = new UserPin();
        userPin.setPinHash("$2a$10$hashedPin");
        when(userPinRepository.findByUserId(1L)).thenReturn(Optional.of(userPin));
        when(passwordEncoder.matches("9999", "$2a$10$hashedPin")).thenReturn(false);

        assertFalse(userPinService.verifyPin(1L, "9999"));
    }

    @Test
    void verifyPin_returnsFalseWhenNoPin() {
        when(userPinRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertFalse(userPinService.verifyPin(1L, "1234"));
    }

    @Test
    void verifyPin_returnsFalseWhenInvalidFormat() {
        assertFalse(userPinService.verifyPin(1L, "abc"));
        assertFalse(userPinService.verifyPin(1L, "123"));
        assertFalse(userPinService.verifyPin(1L, null));
    }
}
