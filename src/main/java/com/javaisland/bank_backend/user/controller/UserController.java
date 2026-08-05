package com.javaisland.bank_backend.user.controller;

import com.javaisland.bank_backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('A')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/export")
    public ResponseEntity<Void> exportCustomers(@RequestParam String fileName) {
        userService.exportCustomersToFile(fileName);
        return ResponseEntity.ok().build();
    }
}