package com.javaisland.bank_backend.employee.controller;

import com.javaisland.bank_backend.account.dto.EmployeeUserDetailDto;
import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.model.LimitChangeRequest;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.repository.LimitChangeRequestRepository;
import com.javaisland.bank_backend.account.service.AccountService;
import com.javaisland.bank_backend.account.service.LimitChangeService;
import com.javaisland.bank_backend.employee.dto.EmployeeRequestDto;
import com.javaisland.bank_backend.user.dto.CustomerListItemDto;
import com.javaisland.bank_backend.user.dto.PasswordChangeRequestDto;
import com.javaisland.bank_backend.user.dto.PendingRegistrationDto;
import com.javaisland.bank_backend.user.model.PasswordChangeRequest;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.PasswordChangeRequestRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.service.PasswordChangeService;
import com.javaisland.bank_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employee/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('D')")
public class EmployeeUserController {

    private final UserService userService;
    private final PasswordChangeService passwordChangeService;
    private final LimitChangeService limitChangeService;
    private final AccountService accountService;
    private final PasswordChangeRequestRepository passwordChangeRequestRepository;
    private final LimitChangeRequestRepository limitChangeRequestRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @GetMapping("/registrations/pending")
    public ResponseEntity<List<PendingRegistrationDto>> getPendingRegistrations() {
        return ResponseEntity.ok(userService.getPendingRegistrations());
    }

    @PutMapping("/registrations/{userId}/validate")
    public ResponseEntity<String> validateRegistration(@PathVariable Long userId) {
        userService.validateRegistration(userId);
        return ResponseEntity.ok("Registrazione validata, conto attivato.");
    }

    @PutMapping("/registrations/{userId}/reject")
    public ResponseEntity<String> rejectRegistration(@PathVariable Long userId) {
        userService.rejectRegistration(userId);
        return ResponseEntity.ok("Registrazione rifiutata.");
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerListItemDto>> getAllCustomersSortedByName() {
        return ResponseEntity.ok(userService.getAllCustomersSortedByName());
    }

    @GetMapping("/{userId}/detail")
    public ResponseEntity<EmployeeUserDetailDto> getUserDetailByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountService.getEmployeeUserDetailByUserId(userId));
    }

    @GetMapping("/password-requests/pending")
    public ResponseEntity<List<PasswordChangeRequestDto>> getPendingPasswordRequests() {
        return ResponseEntity.ok(passwordChangeService.getPendingRequests());
    }

    @PutMapping("/password-requests/{requestId}/approve")
    public ResponseEntity<String> approvePasswordRequest(@PathVariable Long requestId) {
        passwordChangeService.approveRequest(requestId);
        return ResponseEntity.ok("Richiesta di cambio password approvata.");
    }

    @PutMapping("/password-requests/{requestId}/reject")
    public ResponseEntity<String> rejectPasswordRequest(@PathVariable Long requestId) {
        passwordChangeService.rejectRequest(requestId);
        return ResponseEntity.ok("Richiesta di cambio password rifiutata.");
    }

    @GetMapping("/limit-requests/pending")
    public ResponseEntity<List<LimitChangeRequest>> getPendingLimitRequests() {
        return ResponseEntity.ok(limitChangeService.getPendingRequests());
    }

    @PutMapping("/limit-requests/{requestId}/approve")
    public ResponseEntity<String> approveLimitRequest(@PathVariable Long requestId) {
        limitChangeService.approveRequest(requestId);
        return ResponseEntity.ok("Richiesta di modifica limite approvata.");
    }

    @PutMapping("/limit-requests/{requestId}/reject")
    public ResponseEntity<String> rejectLimitRequest(@PathVariable Long requestId) {
        limitChangeService.rejectRequest(requestId);
        return ResponseEntity.ok("Richiesta di modifica limite rifiutata.");
    }

    @GetMapping("/all-requests")
    public ResponseEntity<List<EmployeeRequestDto>> getAllRequests() {
        List<EmployeeRequestDto> all = new ArrayList<>();

        passwordChangeRequestRepository.findAll().forEach(req -> {
            User user = userRepository.findById(req.getUserId()).orElse(null);
            all.add(EmployeeRequestDto.builder()
                    .id(req.getId())
                    .type("PASSWORD_CHANGE")
                    .status(req.getStatus())
                    .description("Cambio password richiesto")
                    .userId(req.getUserId())
                    .userFirstName(user != null ? user.getFirstName() : null)
                    .userLastName(user != null ? user.getLastName() : null)
                    .userEmail(user != null ? user.getEmail() : null)
                    .createdAt(req.getCreatedAt())
                    .processedAt(req.getProcessedAt())
                    .build());
        });

        limitChangeRequestRepository.findAll().forEach(req -> {
            User user = userRepository.findById(req.getUserId()).orElse(null);
            all.add(EmployeeRequestDto.builder()
                    .id(req.getId())
                    .type("LIMIT_CHANGE")
                    .status(req.getStatus())
                    .description(req.getLimitTypeName() + " — €" + req.getRequestedAmount())
                    .userId(req.getUserId())
                    .userFirstName(user != null ? user.getFirstName() : null)
                    .userLastName(user != null ? user.getLastName() : null)
                    .userEmail(user != null ? user.getEmail() : null)
                    .accountNumber(req.getAccountNumber())
                    .limitTypeName(req.getLimitTypeName())
                    .requestedAmount(req.getRequestedAmount())
                    .createdAt(req.getCreatedAt())
                    .processedAt(req.getProcessedAt())
                    .build());
        });

        accountRepository.findAll().forEach(acc -> {
            User user = acc.getUser();
            String iban = acc.getAccountNumber();
            if (acc.getStatusId() == 1) {
                all.add(EmployeeRequestDto.builder()
                        .id(acc.getId())
                        .type("ACCOUNT_OPENING")
                        .status("PENDING")
                        .description("Apertura conto — IBAN: " + iban)
                        .userId(user.getId())
                        .userFirstName(user.getFirstName())
                        .userLastName(user.getLastName())
                        .userEmail(user.getEmail())
                        .accountNumber(iban)
                        .createdAt(acc.getCreatedAt())
                        .build());
            } else if (acc.getStatusId() == 3 && acc.getClosureRequestedAt() != null) {
                all.add(EmployeeRequestDto.builder()
                        .id(acc.getId())
                        .type("ACCOUNT_CLOSURE")
                        .status("PENDING")
                        .description("Chiusura conto — IBAN: " + iban)
                        .userId(user.getId())
                        .userFirstName(user.getFirstName())
                        .userLastName(user.getLastName())
                        .userEmail(user.getEmail())
                        .accountNumber(iban)
                        .createdAt(acc.getClosureRequestedAt())
                        .build());
            } else if (acc.getStatusId() == 4) {
                all.add(EmployeeRequestDto.builder()
                        .id(acc.getId())
                        .type("ACCOUNT_CLOSURE")
                        .status("APPROVED")
                        .description("Chiusura conto — IBAN: " + iban)
                        .userId(user.getId())
                        .userFirstName(user.getFirstName())
                        .userLastName(user.getLastName())
                        .userEmail(user.getEmail())
                        .accountNumber(iban)
                        .createdAt(acc.getCreatedAt())
                        .processedAt(acc.getClosedAt())
                        .build());
            }
        });

        all.sort(Comparator.comparing(EmployeeRequestDto::getCreatedAt).reversed());
        return ResponseEntity.ok(all);
    }
}
