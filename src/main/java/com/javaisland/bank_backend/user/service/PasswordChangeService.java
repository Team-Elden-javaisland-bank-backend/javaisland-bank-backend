package com.javaisland.bank_backend.user.service;

import com.javaisland.bank_backend.auth.service.KeycloakAdminService;
import com.javaisland.bank_backend.auth.service.RegistrationService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.user.dto.PasswordChangeRequestCreateDto;
import com.javaisland.bank_backend.user.dto.PasswordChangeRequestDto;
import com.javaisland.bank_backend.user.model.PasswordChangeRequest;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.PasswordChangeRequestRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordChangeService {

    private final PasswordChangeRequestRepository passwordChangeRequestRepository;
    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final NotificationService notificationService;

    public void requestPasswordChange(Long userId, PasswordChangeRequestCreateDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        String hashedCurrentPassword = RegistrationService.hashPassword(request.getCurrentPassword());
        if (!hashedCurrentPassword.equals(user.getPassword())) {
            throw new ApiBankException("La password attuale non è corretta.", "INVALID_CURRENT_PASSWORD");
        }

        String hashedNewPassword = RegistrationService.hashPassword(request.getNewPassword());
        if (hashedNewPassword.equals(user.getPassword())) {
            throw new ApiBankException("La nuova password non può essere uguale a quella attuale.", "SAME_PASSWORD");
        }

        String newPwd = request.getNewPassword();
        if (newPwd.length() < 8
                || !newPwd.matches(".*[A-Z].*")
                || !newPwd.matches(".*[a-z].*")
                || !newPwd.matches(".*\\d.*")
                || !newPwd.matches(".*[^a-zA-Z0-9].*")) {
            throw new ApiBankException(
                    "La password deve contenere almeno 8 caratteri, 1 maiuscola, 1 minuscola, 1 numero e 1 carattere speciale.",
                    "INVALID_PASSWORD_FORMAT");
        }

        passwordChangeRequestRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING")
                .ifPresent(existing -> {
                    throw new ApiBankException("Richiesta già in sospeso.", "PENDING_REQUEST_EXISTS");
                });

        PasswordChangeRequest changeRequest = PasswordChangeRequest.builder()
                .userId(userId)
                .newPlainPassword(request.getNewPassword())
                .status("PENDING")
                .build();

        passwordChangeRequestRepository.save(changeRequest);
        notificationService.send(userId, "PASSWORD_CHANGE", "Richiesta di cambio password inviata. In attesa di approvazione.", "NOTIF_PWD_CHANGE_REQUESTED", null);
        log.info("Password change request created for user id={}", userId);
    }

    public List<PasswordChangeRequestDto> getPendingRequests() {
        List<PasswordChangeRequest> pendingRequests = passwordChangeRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING");

        return pendingRequests.stream()
                .map(req -> {
                    User user = userRepository.findById(req.getUserId()).orElse(null);
                    return PasswordChangeRequestDto.builder()
                            .id(req.getId())
                            .userId(req.getUserId())
                            .userFirstName(user != null ? user.getFirstName() : null)
                            .userLastName(user != null ? user.getLastName() : null)
                            .userEmail(user != null ? user.getEmail() : null)
                            .newPlainPassword(req.getNewPlainPassword())
                            .status(req.getStatus())
                            .createdAt(req.getCreatedAt())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void approveRequest(Long requestId) {
        PasswordChangeRequest request = passwordChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiBankException("Richiesta non trovata.", "REQUEST_NOT_FOUND"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ApiBankException("La richiesta non è in stato PENDING.", "INVALID_REQUEST_STATE");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        String hashedNewPassword = RegistrationService.hashPassword(request.getNewPlainPassword());
        user.setPassword(hashedNewPassword);
        user.setPasswordChangedAt(LocalDateTime.now(ZoneId.of("Europe/Rome")));
        userRepository.save(user);

        keycloakAdminService.resetPassword(user.getKeycloakId(), request.getNewPlainPassword());
        keycloakAdminService.logoutUser(user.getKeycloakId());

        request.setStatus("APPROVED");
        request.setProcessedAt(LocalDateTime.now(ZoneId.of("Europe/Rome")));
        passwordChangeRequestRepository.save(request);

        notificationService.send(user.getId(), "PASSWORD_CHANGE", "La tua richiesta di cambio password è stata approvata.", "NOTIF_PWD_CHANGE_APPROVED", null);

        log.info("Password change request id={} approved for user id={}", requestId, user.getId());
    }

    @Transactional
    public void rejectRequest(Long requestId) {
        PasswordChangeRequest request = passwordChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiBankException("Richiesta non trovata.", "REQUEST_NOT_FOUND"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ApiBankException("La richiesta non è in stato PENDING.", "INVALID_REQUEST_STATE");
        }

        request.setStatus("REJECTED");
        request.setProcessedAt(LocalDateTime.now(ZoneId.of("Europe/Rome")));
        passwordChangeRequestRepository.save(request);

        notificationService.send(request.getUserId(), "PASSWORD_CHANGE", "La tua richiesta di cambio password è stata rifiutata.", "NOTIF_PWD_CHANGE_REJECTED", null);

        log.info("Password change request id={} rejected for user id={}", requestId, request.getUserId());
    }

    public List<PasswordChangeRequestDto> getRequestsByUserId(Long userId) {
        return passwordChangeRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(req -> PasswordChangeRequestDto.builder()
                        .id(req.getId())
                        .userId(req.getUserId())
                        .userFirstName(null)
                        .userLastName(null)
                        .userEmail(null)
                        .newPlainPassword(null)
                        .status(req.getStatus())
                        .createdAt(req.getCreatedAt())
                        .processedAt(req.getProcessedAt())
                        .build())
                .toList();
    }
}
