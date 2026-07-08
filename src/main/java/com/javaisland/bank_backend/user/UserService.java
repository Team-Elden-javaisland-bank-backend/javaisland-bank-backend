package com.javaisland.bank_backend.user;

import com.javaisland.bank_backend.user.RoleTypeRepository;
import com.javaisland.bank_backend.user.UserStatusRepository;
import com.javaisland.bank_backend.exception.ApiBankException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;
    private final RoleTypeRepository roleTypeRepository;

    public UserService(UserRepository userRepository,
                       UserStatusRepository userStatusRepository,
                       RoleTypeRepository roleTypeRepository) {
        this.userRepository = userRepository;
        this.userStatusRepository = userStatusRepository;
        this.roleTypeRepository = roleTypeRepository;
    }

    @Transactional
    public User registerUser(UserRegisterDTO dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new ApiBankException("Username già in uso nel sistema.");
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ApiBankException("Email già registrata nel sistema.");
        }

        if (userRepository.findByKeycloakId(dto.getKeycloakId()).isPresent()) {
            throw new ApiBankException("Questo account Keycloak è già censito.");
        }

        var pendingStatus = userStatusRepository.findByStatusName("PENDING")
                .orElseThrow(() -> new ApiBankException("Stato utente PENDING non configurato."));
        var customerRole = roleTypeRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new ApiBankException("Ruolo CUSTOMER non configurato."));

        User user = new User();
        user.setKeycloakId(dto.getKeycloakId());
        user.setUsername(dto.getUsername());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setBirthDate(dto.getBirthDate());
        user.setEmail(dto.getEmail());
        user.setBranchCode(dto.getBranchCode());
        user.setStatus(pendingStatus);
        user.setRoleType(customerRole);

        return userRepository.save(user);
    }

    public void exportCustomersToFile(String filePath) {
        List<User> users = userRepository.findAll();

        var customerRole = roleTypeRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new ApiBankException("Ruolo CUSTOMER non configurato."));

        List<User> sortedCustomers = users.stream()
                .filter(u -> u.getRoleType().getId().equals(customerRole.getId()))
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .toList();

        try (FileWriter fileWriter = new FileWriter(filePath);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            printWriter.println("=== ELENCO CORRENTISTI ORDINATO ===");
            for (User customer : sortedCustomers) {
                printWriter.printf("Nome: %s | Cognome: %s | Email: %s | Stato: %s%n",
                        customer.getFirstName(),
                        customer.getLastName(),
                        customer.getEmail(),
                        customer.getStatus().getStatusName());
            }

        } catch (IOException e) {
            throw new ApiBankException("Errore durante la scrittura del file dei correntisti.");
        }
    }
}