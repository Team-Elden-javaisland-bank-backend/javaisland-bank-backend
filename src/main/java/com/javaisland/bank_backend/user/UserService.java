package com.javaisland.bank_backend.user;

import com.javaisland.bank_backend.exception.ApiBankException;
import org.springframework.stereotype.Service;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 🧠 1. LOGICA DI REGISTRAZIONE UTENTE
    public User registerUser(User user) {
        // Controllo se lo username esiste già
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new ApiBankException("Username già in uso nel sistema.");
        }

        // Controllo se l'email esiste già
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ApiBankException("Email già registrata nel sistema.");
        }

        // ✅ Impostiamo lo stato usando l'Enum
        user.setStatus(UserStatus.PENDING);

        // Impostiamo il ruolo iniziale a 1 (CUSTOMER)
        user.setRoleTypeId(1);

        return userRepository.save(user);
    }

    // 🧠 2. STAMPA SU FILE DEI CORRENTISTI ORDINATI (Funzione per Dipendente)
    public void exportCustomersToFile(String filePath) {
        // Recuperiamo tutti gli utenti dal database
        List<User> users = userRepository.findAll();

        // Filtriamo solo i clienti (role_type_id = 1) e li ordiniamo per Nome + Cognome con Java
        List<User> sortedCustomers = users.stream()
                .filter(u -> u.getRoleTypeId() == 1)
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .toList();

        // Scrittura fisica sul file di testo
        try (FileWriter fileWriter = new FileWriter(filePath);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            printWriter.println("=== ELENCO CORRENTISTI ORDINATO ===");
            for (User customer : sortedCustomers) {
                // ✅ Modificato %d in %s e getStatusId() in getStatus()
                printWriter.printf("Nome: %s | Cognome: %s | Email: %s | Stato: %s%n",
                        customer.getFirstName(),
                        customer.getLastName(),
                        customer.getEmail(),
                        customer.getStatus());
            }

        } catch (IOException e) {
            throw new ApiBankException("Errore durante la scrittura del file dei correntisti.");
        }
    }
}