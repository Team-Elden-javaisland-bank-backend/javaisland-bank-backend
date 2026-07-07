package com.javaisland.bank_backend.employee;

import com.javaisland.bank_backend.user.CustomerListItemDto;
import com.javaisland.bank_backend.user.PendingRegistrationDto;
import com.javaisland.bank_backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeUserController {

    private final UserService userService;

    @GetMapping("/registrations/pending")
    public ResponseEntity<List<PendingRegistrationDto>> getPendingRegistrations() {
        return ResponseEntity.ok(userService.getPendingRegistrations());
    }

    @PutMapping("/registrations/{userId}/validate")
    public ResponseEntity<String> validateRegistration(@PathVariable Long userId) {
        userService.validateRegistration(userId);
        return ResponseEntity.ok("Registration validated, account activated.");
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerListItemDto>> getAllCustomersSortedByName() {
        return ResponseEntity.ok(userService.getAllCustomersSortedByName());
    }
}
