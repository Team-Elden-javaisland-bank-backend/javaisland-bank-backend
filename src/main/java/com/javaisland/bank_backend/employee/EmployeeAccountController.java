package com.javaisland.bank_backend.employee;

import com.javaisland.bank_backend.account.AccountResponseDto;
import com.javaisland.bank_backend.account.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employee/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeAccountController {

    private final AccountService accountService;

    @PutMapping("/{accountNumber}/activate")
    public ResponseEntity<String> activate(@PathVariable String accountNumber) {
        accountService.activateAccount(accountNumber);
        return ResponseEntity.ok("Account " + accountNumber + " activated.");
    }

    @PutMapping("/{accountNumber}/closure/validate")
    public ResponseEntity<String> validateClosure(@PathVariable String accountNumber) {
        accountService.validateClosure(accountNumber);
        return ResponseEntity.ok("Account " + accountNumber + " closed.");
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponseDto> getDetail(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountDetailAsEmployee(accountNumber));
    }
}
