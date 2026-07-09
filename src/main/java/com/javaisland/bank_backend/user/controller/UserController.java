package com.javaisland.bank_backend.user.controller;

import com.javaisland.bank_backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportCustomers(@RequestParam String filePath) {
        userService.exportCustomersToFile(filePath);
        return ResponseEntity.ok("File esportato con successo in: " + filePath);
    }
}