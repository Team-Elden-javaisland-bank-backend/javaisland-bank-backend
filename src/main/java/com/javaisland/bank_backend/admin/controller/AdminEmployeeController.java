package com.javaisland.bank_backend.admin.controller;

import com.javaisland.bank_backend.admin.dto.CreateEmployeeRequestDto;
import com.javaisland.bank_backend.admin.dto.EmployeeDetailDto;
import com.javaisland.bank_backend.admin.dto.EmployeeListItemDto;
import com.javaisland.bank_backend.admin.service.AdminEmployeeService;
import com.javaisland.bank_backend.common.PageResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<PageResponseDto<EmployeeListItemDto>> listEmployees(
            @PageableDefault(size = 20, sort = "firstName") Pageable pageable) {
        List<EmployeeListItemDto> fullList = adminEmployeeService.getAllEmployees();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), fullList.size());
        List<EmployeeListItemDto> paginatedList = fullList.subList(start, end);
        return ResponseEntity.ok(PageResponseDto.from(new PageImpl<>(paginatedList, pageable, fullList.size())));
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
    public ResponseEntity<Void> suspendEmployee(@PathVariable Long userId) {
        adminEmployeeService.suspendEmployee(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/activate")
    public ResponseEntity<Void> activateEmployee(@PathVariable Long userId) {
        adminEmployeeService.activateEmployee(userId);
        return ResponseEntity.ok().build();
    }
}
