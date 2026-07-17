package com.javaisland.bank_backend.admin.service;

import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.admin.dto.AdminDashboardDto;
import com.javaisland.bank_backend.transaction.repository.TransactionRepository;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final RoleTypeRepository roleTypeRepository;
    private final UserStatusRepository userStatusRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboardStats() {
        var customerRole = roleTypeRepository.findByRoleName("C").orElse(null);
        var employeeRole = roleTypeRepository.findByRoleName("D").orElse(null);
        var pendingStatus = userStatusRepository.findByUserStatus("PENDING").orElse(null);

        long totalCustomers = customerRole != null ? userRepository.countByRoleType(customerRole) : 0;
        long totalEmployees = employeeRole != null ? userRepository.countByRoleType(employeeRole) : 0;
        long pendingRegistrations = pendingStatus != null ? userRepository.countByStatus(pendingStatus) : 0;

        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countByStatusId(AccountStatus.ACTIVE);
        long frozenAccounts = accountRepository.countByStatusId(AccountStatus.FROZEN);

        long totalTransactions = transactionRepository.count();
        BigDecimal totalBalance = accountRepository.sumBalanceByStatusId(AccountStatus.ACTIVE);

        return AdminDashboardDto.builder()
                .totalCustomers(totalCustomers)
                .totalEmployees(totalEmployees)
                .totalAccounts(totalAccounts)
                .activeAccounts(activeAccounts)
                .frozenAccounts(frozenAccounts)
                .pendingRegistrations(pendingRegistrations)
                .totalTransactions(totalTransactions)
                .totalBalance(totalBalance != null ? totalBalance : BigDecimal.ZERO)
                .build();
    }
}
