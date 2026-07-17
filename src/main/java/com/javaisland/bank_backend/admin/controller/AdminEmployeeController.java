package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.admin.dto.CreateEmployeeRequestDto;
import com.javaisland.bank_backend.admin.dto.EmployeeDetailDto;
import com.javaisland.bank_backend.admin.dto.EmployeeListItemDto;
import com.javaisland.bank_backend.admin.service.AdminEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('A')")
public class AdminEmployeeController {

    private final AdminEmployeeService adminEmployeeService;

    @GetMapping
    public ResponseEntity<List<EmployeeListItemDto>> listEmployees() {
        return ResponseEntity.ok(adminEmployeeService.getAllEmployees());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<EmployeeDetailDto> getEmployeeDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(adminEmployeeService.getEmployeeDetail(userId));
    }

    @PostMapping
    public ResponseEntity<EmployeeListItemDto> createEmployee(@Valid @RequestBody CreateEmployeeRequestDto request) {
        return ResponseEntity.ok(adminEmployeeService.createEmployee(request));
    }

    @PutMapping("/{userId}/suspend")
    public ResponseEntity<String> suspendEmployee(@PathVariable Long userId) {
        adminEmployeeService.suspendEmployee(userId);
        return ResponseEntity.ok("Dipendente sospeso con successo.");
    }

    @PutMapping("/{userId}/activate")
    public ResponseEntity<String> activateEmployee(@PathVariable Long userId) {
        adminEmployeeService.activateEmployee(userId);
        return ResponseEntity.ok("Dipendente attivato con successo.");
    }
}
