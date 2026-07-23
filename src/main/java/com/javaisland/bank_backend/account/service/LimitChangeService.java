package com.javaisland.bank_backend.account.service;

import com.javaisland.bank_backend.account.dto.AccountLimitResponseDto;
import com.javaisland.bank_backend.account.dto.LimitChangeRequestCreateDto;
import com.javaisland.bank_backend.account.model.AccountLimit;
import com.javaisland.bank_backend.account.model.LimitChangeRequest;
import com.javaisland.bank_backend.account.model.LimitType;
import com.javaisland.bank_backend.account.repository.AccountLimitRepository;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.repository.LimitChangeRequestRepository;
import com.javaisland.bank_backend.account.repository.LimitTypeRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.notification.service.NotificationService;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LimitChangeService {

    private final LimitChangeRequestRepository limitChangeRequestRepository;
    private final AccountLimitRepository accountLimitRepository;
    private final AccountRepository accountRepository;
    private final LimitTypeRepository limitTypeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AccountLimitService accountLimitService;

    @Transactional
    public void requestLimitChange(Long userId, LimitChangeRequestCreateDto request) {
        var account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ApiBankException("Conto non trovato.", "ACCOUNT_NOT_FOUND"));

        if (!account.getUser().getId().equals(userId)) {
            throw new ApiBankException("Conto non appartiene all'utente.", "ACCOUNT_NOT_OWNED");
        }

        var limitType = limitTypeRepository.findByLimitName(request.getLimitType())
                .orElseThrow(() -> new ApiBankException("Tipo limite non valido.", "INVALID_LIMIT_TYPE"));

        Optional<AccountLimit> existingLimit = accountLimitRepository
                .findByAccountAndLimitType(account, limitType);

        BigDecimal currentAmount = existingLimit.map(AccountLimit::getMaxAmount).orElse(BigDecimal.ZERO);

        if (limitType.getChangePolicy() == LimitType.ChangePolicy.USER_FULL) {
            throw new ApiBankException("Questo limite puo essere modificato direttamente.", "LIMIT_FULL_ACCESS");
        }

        if (limitType.getChangePolicy() == LimitType.ChangePolicy.USER_LOWER_ONLY) {
            if (request.getRequestedAmount().compareTo(currentAmount) <= 0) {
                throw new ApiBankException(
                        "Per questo limite puoi solo richiedere un aumento. Per diminuire, usa la modifica diretta.",
                        "LOWER_ONLY_INCREASE_REQUIRED");
            }
        }

        if (request.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiBankException("L'importo richiesto deve essere maggiore di zero.", "INVALID_AMOUNT");
        }

        if (limitChangeRequestRepository.existsByAccountNumberAndLimitTypeNameAndStatus(
                request.getAccountNumber(), request.getLimitType(), "PENDING")) {
            throw new ApiBankException(
                    "Hai già una richiesta in corso per questo limite. Attendi che venga elaborata.",
                    "PENDING_REQUEST_EXISTS");
        }

        limitChangeRequestRepository.save(LimitChangeRequest.builder()
                .userId(userId)
                .accountNumber(request.getAccountNumber())
                .limitTypeName(request.getLimitType())
                .requestedAmount(request.getRequestedAmount())
                .currentAmount(currentAmount)
                .status("PENDING")
                .build());

        notificationService.send(userId, "LIMIT_CHANGE",
                "Richiesta di modifica limite " + request.getLimitType() + " inviata. In attesa di approvazione.", "NOTIF_LIMIT_CHANGE_REQUESTED", "[\"" + request.getLimitType() + "\"]");

        log.info("Limit change request created for user id={}, limit={}", userId, request.getLimitType());
    }

    public List<LimitChangeRequest> getPendingRequests() {
        return limitChangeRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    public List<LimitChangeRequest> getRequestsByUserId(Long userId) {
        return limitChangeRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void approveRequest(Long requestId) {
        LimitChangeRequest request = limitChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiBankException("Richiesta non trovata.", "REQUEST_NOT_FOUND"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ApiBankException("Richiesta non e in stato PENDING.", "REQUEST_NOT_PENDING");
        }

        var account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ApiBankException("Conto non trovato.", "ACCOUNT_NOT_FOUND"));

        var limitType = limitTypeRepository.findByLimitName(request.getLimitTypeName())
                .orElseThrow(() -> new ApiBankException("Tipo limite non valido.", "INVALID_LIMIT_TYPE"));

        Optional<AccountLimit> existingLimit = accountLimitRepository
                .findByAccountAndLimitType(account, limitType);

        if (existingLimit.isPresent()) {
            existingLimit.get().setMaxAmount(request.getRequestedAmount());
            accountLimitRepository.save(existingLimit.get());
        } else {
            accountLimitRepository.save(AccountLimit.builder()
                    .account(account)
                    .limitType(limitType)
                    .maxAmount(request.getRequestedAmount())
                    .build());
        }

        request.setStatus("APPROVED");
        request.setProcessedAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Europe/Rome")));
        limitChangeRequestRepository.save(request);

        notificationService.send(request.getUserId(), "LIMIT_CHANGE",
                "La tua richiesta di modifica limite " + request.getLimitTypeName() + " e stata approvata.", "NOTIF_LIMIT_CHANGE_APPROVED", "[\"" + request.getLimitTypeName() + "\"]");

        log.info("Limit change request id={} approved", requestId);
    }

    @Transactional
    public void rejectRequest(Long requestId) {
        LimitChangeRequest request = limitChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiBankException("Richiesta non trovata.", "REQUEST_NOT_FOUND"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new ApiBankException("Richiesta non e in stato PENDING.", "REQUEST_NOT_PENDING");
        }

        request.setStatus("REJECTED");
        request.setProcessedAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Europe/Rome")));
        limitChangeRequestRepository.save(request);

        notificationService.send(request.getUserId(), "LIMIT_CHANGE",
                "La tua richiesta di modifica limite " + request.getLimitTypeName() + " e stata rifiutata.", "NOTIF_LIMIT_CHANGE_REJECTED", "[\"" + request.getLimitTypeName() + "\"]");

        log.info("Limit change request id={} rejected", requestId);
    }
}
