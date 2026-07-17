package com.javaisland.bank_backend.user.service;

import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.model.UserPin;
import com.javaisland.bank_backend.user.repository.UserPinRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPinService {

    private final UserPinRepository userPinRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void setupPin(Long userId, String pin) {
        if (pin == null || !pin.matches("^\\d{4}$")) {
            throw new ApiBankException("Il PIN deve essere di 4 cifre.", "INVALID_PIN_FORMAT");
        }

        if (userPinRepository.existsByUserId(userId)) {
            throw new ApiBankException("PIN già configurato. Usa l'endpoint di modifica.", "PIN_ALREADY_EXISTS");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        UserPin userPin = new UserPin();
        userPin.setUser(user);
        userPin.setPinHash(passwordEncoder.encode(pin));
        userPinRepository.save(userPin);
    }

    @Transactional(readOnly = true)
    public boolean hasPin(Long userId) {
        return userPinRepository.existsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean verifyPin(Long userId, String pin) {
        if (pin == null || !pin.matches("^\\d{4}$")) {
            return false;
        }

        return userPinRepository.findByUserId(userId)
                .map(userPin -> passwordEncoder.matches(pin, userPin.getPinHash()))
                .orElse(false);
    }
}
