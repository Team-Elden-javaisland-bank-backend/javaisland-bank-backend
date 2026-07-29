package com.javaisland.bank_backend.common;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    private final UserRepository userRepository;

    public SecurityUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getUserId(Jwt jwt) {
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"))
                .getId();
    }

    public User getUser(Jwt jwt) {
        return userRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));
    }

    public void assertOwnership(Account account, Long userId) {
        if (!account.getUser().getId().equals(userId)) {
            throw new ApiBankException("Il conto " + account.getAccountNumber() + " non appartiene all'utente corrente.", "FORBIDDEN");
        }
    }
}
