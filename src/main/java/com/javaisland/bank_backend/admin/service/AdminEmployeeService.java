package com.javaisland.bank_backend.admin.service;

import com.javaisland.bank_backend.admin.dto.CreateEmployeeRequestDto;
import com.javaisland.bank_backend.admin.dto.EmployeeDetailDto;
import com.javaisland.bank_backend.admin.dto.EmployeeListItemDto;
import com.javaisland.bank_backend.auth.service.KeycloakAdminService;
import com.javaisland.bank_backend.exception.ApiBankException;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminEmployeeService {

    private final UserRepository userRepository;
    private final RoleTypeRepository roleTypeRepository;
    private final UserStatusRepository userStatusRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Transactional(readOnly = true)
    public List<EmployeeListItemDto> getAllEmployees() {
        var employeeRole = roleTypeRepository.findByRoleName("D")
                .orElseThrow(() -> new ApiBankException("Ruolo D non configurato.", "ROLE_NOT_FOUND"));
        return userRepository.findByRoleTypeOrderByFirstNameAscLastNameAsc(employeeRole)
                .stream().map(u -> EmployeeListItemDto.builder()
                        .userId(u.getId())
                        .username(u.getUsername())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .status(u.getStatus().getUserStatus())
                        .createdAt(u.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDetailDto getEmployeeDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));
        return EmployeeDetailDto.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .status(user.getStatus().getUserStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public EmployeeListItemDto createEmployee(CreateEmployeeRequestDto request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiBankException("Email già registrata.", "EMAIL_EXISTS");
        }

        var employeeRole = roleTypeRepository.findByRoleName("D")
                .orElseThrow(() -> new ApiBankException("Ruolo D non configurato.", "ROLE_NOT_FOUND"));
        var activeStatus = userStatusRepository.findByUserStatus("ACTIVE")
                .orElseThrow(() -> new ApiBankException("Stato ACTIVE non configurato."));

        String keycloakId;
        try {
            keycloakId = keycloakAdminService.createUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    true);
        } catch (Exception e) {
            throw new ApiBankException("Creazione utente Keycloak fallita.", "KEYCLOAK_CREATION_FAILED");
        }

        try {
            keycloakAdminService.assignRole(keycloakId, "D");
        } catch (Exception e) {
            keycloakAdminService.deleteUser(keycloakId);
            throw new ApiBankException("Assegnazione ruolo dipendente fallita.", "ROLE_ASSIGNMENT_FAILED");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail());
        user.setKeycloakId(keycloakId);
        user.setRoleType(employeeRole);
        user.setStatus(activeStatus);
        user = userRepository.save(user);

        log.info("Admin created employee: id={}, email={}", user.getId(), request.getEmail());

        return EmployeeListItemDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .status(user.getStatus().getUserStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public void suspendEmployee(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        var employeeRole = roleTypeRepository.findByRoleName("D")
                .orElseThrow(() -> new ApiBankException("Ruolo D non configurato.", "ROLE_NOT_FOUND"));
        if (!user.getRoleType().getId().equals(employeeRole.getId())) {
            throw new ApiBankException("L'utente non è un dipendente.", "NOT_EMPLOYEE");
        }

        var suspendedStatus = userStatusRepository.findByUserStatus("SUSPENDED")
                .orElseThrow(() -> new ApiBankException("Stato SUSPENDED non configurato."));
        user.setStatus(suspendedStatus);
        userRepository.save(user);

        if (user.getKeycloakId() != null) {
            keycloakAdminService.setUserEnabled(user.getKeycloakId(), false);
        }

        log.info("Admin suspended employee: id={}", userId);
    }

    @Transactional
    public void activateEmployee(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiBankException("Utente non trovato.", "USER_NOT_FOUND"));

        var employeeRole = roleTypeRepository.findByRoleName("D")
                .orElseThrow(() -> new ApiBankException("Ruolo D non configurato.", "ROLE_NOT_FOUND"));
        if (!user.getRoleType().getId().equals(employeeRole.getId())) {
            throw new ApiBankException("L'utente non è un dipendente.", "NOT_EMPLOYEE");
        }

        var activeStatus = userStatusRepository.findByUserStatus("ACTIVE")
                .orElseThrow(() -> new ApiBankException("Stato ACTIVE non configurato."));
        user.setStatus(activeStatus);
        userRepository.save(user);

        if (user.getKeycloakId() != null) {
            keycloakAdminService.setUserEnabled(user.getKeycloakId(), true);
        }

        log.info("Admin activated employee: id={}", userId);
    }
}
