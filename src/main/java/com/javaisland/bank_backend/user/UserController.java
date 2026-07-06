package com.javaisland.bank_backend.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 🧠 1. ENDPOINT PER LA REGISTRAZIONE (Accessibile da tutti)
    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {
        User registeredUser = userService.registerUser(user);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    // 🧠 2. ENDPOINT PER ESPORTARE I CORRENTISTI (Funzione per Dipendente)
    @GetMapping("/export")
    public ResponseEntity<String> exportCustomers(@RequestParam String filePath) {
        userService.exportCustomersToFile(filePath);
        return ResponseEntity.ok("File esportato con successo in: " + filePath);
    }
}