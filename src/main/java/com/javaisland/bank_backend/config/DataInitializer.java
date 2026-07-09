package com.javaisland.bank_backend.config;

import com.javaisland.bank_backend.account.model.Account;
import com.javaisland.bank_backend.account.repository.AccountRepository;
import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountStatusRepository;
import com.javaisland.bank_backend.account.model.LimitType;
import com.javaisland.bank_backend.account.repository.LimitTypeRepository;
import com.javaisland.bank_backend.auth.service.RegistrationService;
import com.javaisland.bank_backend.card.model.CardStatus;
import com.javaisland.bank_backend.card.repository.CardStatusRepository;
import com.javaisland.bank_backend.card.model.CardType;
import com.javaisland.bank_backend.card.repository.CardTypeRepository;
import com.javaisland.bank_backend.transaction.model.TransactionStatus;
import com.javaisland.bank_backend.transaction.repository.TransactionStatusRepository;
import com.javaisland.bank_backend.transaction.model.TransactionType;
import com.javaisland.bank_backend.transaction.repository.TransactionTypeRepository;
import com.javaisland.bank_backend.user.model.RoleType;
import com.javaisland.bank_backend.user.repository.RoleTypeRepository;
import com.javaisland.bank_backend.user.model.User;
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.model.UserStatus;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserStatusRepository userStatusRepository;
    private final RoleTypeRepository roleTypeRepository;
    private final CardStatusRepository cardStatusRepository;
    private final CardTypeRepository cardTypeRepository;
    private final AccountStatusRepository accountStatusRepository;
    private final LimitTypeRepository limitTypeRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public DataInitializer(UserStatusRepository userStatusRepository,
                           RoleTypeRepository roleTypeRepository,
                           CardStatusRepository cardStatusRepository,
                           CardTypeRepository cardTypeRepository,
                           AccountStatusRepository accountStatusRepository,
                           LimitTypeRepository limitTypeRepository,
                           TransactionTypeRepository transactionTypeRepository,
                           TransactionStatusRepository transactionStatusRepository,
                           UserRepository userRepository,
                           AccountRepository accountRepository) {
        this.userStatusRepository = userStatusRepository;
        this.roleTypeRepository = roleTypeRepository;
        this.cardStatusRepository = cardStatusRepository;
        this.cardTypeRepository = cardTypeRepository;
        this.accountStatusRepository = accountStatusRepository;
        this.limitTypeRepository = limitTypeRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionStatusRepository = transactionStatusRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(String... args) {
        seedUserStatuses();
        seedRoleTypes();
        seedCardStatuses();
        seedCardTypes();
        seedAccountStatuses();
        seedLimitTypes();
        seedTransactionTypes();
        seedTransactionStatuses();
    }

    private void seedUserStatuses() {
        if (userStatusRepository.count() > 0) return;
        userStatusRepository.save(new UserStatus(null, "PENDING"));
        userStatusRepository.save(new UserStatus(null, "ACTIVE"));
        userStatusRepository.save(new UserStatus(null, "ANNULLED"));
        userStatusRepository.save(new UserStatus(null, "SUSPENDED"));
    }

    private void seedRoleTypes() {
        if (roleTypeRepository.count() == 0) {
            roleTypeRepository.save(new RoleType(null, "C"));
            roleTypeRepository.save(new RoleType(null, "D"));
        } else {
            roleTypeRepository.findByRoleName("CUSTOMER").ifPresent(r -> { r.setRoleName("C"); roleTypeRepository.save(r); });
            roleTypeRepository.findByRoleName("EMPLOYEE").ifPresent(r -> { r.setRoleName("D"); roleTypeRepository.save(r); });
        }
    }

    private void seedCardStatuses() {
        if (cardStatusRepository.count() > 0) return;
        cardStatusRepository.save(new CardStatus(null, "INACTIVE"));
        cardStatusRepository.save(new CardStatus(null, "ACTIVE"));
        cardStatusRepository.save(new CardStatus(null, "BLOCKED"));
    }

    private void seedCardTypes() {
        if (cardTypeRepository.count() > 0) return;
        cardTypeRepository.save(new CardType(null, "DEBIT"));
        cardTypeRepository.save(new CardType(null, "CREDIT"));
    }

    private void seedAccountStatuses() {
        if (accountStatusRepository.count() > 0) return;
        accountStatusRepository.save(new AccountStatus(null, "INACTIVE"));
        accountStatusRepository.save(new AccountStatus(null, "ACTIVE"));
        accountStatusRepository.save(new AccountStatus(null, "FROZEN"));
        accountStatusRepository.save(new AccountStatus(null, "CLOSED"));
    }

    private void seedLimitTypes() {
        if (limitTypeRepository.count() > 0) return;
        limitTypeRepository.save(new LimitType(null, "DAILY_TRANSFER", "Maximum daily transfer amount"));
        limitTypeRepository.save(new LimitType(null, "MONTHLY_TRANSFER", "Maximum monthly transfer amount"));
        limitTypeRepository.save(new LimitType(null, "ATM_WITHDRAWAL", "Maximum ATM withdrawal per transaction"));
        limitTypeRepository.save(new LimitType(null, "POS_SPENDING", "Maximum POS spending per transaction"));
    }

    private void seedTransactionTypes() {
        if (transactionTypeRepository.count() > 0) return;
        transactionTypeRepository.save(new TransactionType(null, "DEPOSIT"));
        transactionTypeRepository.save(new TransactionType(null, "WITHDRAWAL"));
        transactionTypeRepository.save(new TransactionType(null, "TRANSFER"));
        transactionTypeRepository.save(new TransactionType(null, "INITIAL_TRANSFER"));
    }

    private void seedTransactionStatuses() {
        if (transactionStatusRepository.count() > 0) return;
        transactionStatusRepository.save(new TransactionStatus(null, "PENDING"));
        transactionStatusRepository.save(new TransactionStatus(null, "COMPLETED"));
        transactionStatusRepository.save(new TransactionStatus(null, "FAILED"));
        transactionStatusRepository.save(new TransactionStatus(null, "REJECTED"));
    }

    private void seedPreconfiguredCustomers() {
        if (userRepository.count() > 0) return;

        var roleC = roleTypeRepository.findByRoleName("C")
                .orElseThrow(() -> new RuntimeException("Role C not found"));
        var roleD = roleTypeRepository.findByRoleName("D")
                .orElseThrow(() -> new RuntimeException("Role D not found"));
        var activeStatus = userStatusRepository.findByStatusName("ACTIVE")
                .orElseThrow(() -> new RuntimeException("Status ACTIVE not found"));
        String hashedPass = RegistrationService.hashPassword("password123");

        User customer1 = new User();
        customer1.setFirstName("Mario");
        customer1.setLastName("Rossi");
        customer1.setBirthDate(java.time.LocalDate.of(1985, 3, 15));
        customer1.setEmail("mario.rossi@example.com");
        customer1.setUsername("mario.rossi@example.com");
        customer1.setPassword(hashedPass);
        customer1.setRoleType(roleC);
        customer1.setStatus(activeStatus);
        customer1 = userRepository.save(customer1);
        Account acc1 = new Account();
        acc1.setAccountNumber("IT" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase());
        acc1.setBalance(new BigDecimal("1500.00"));
        acc1.setStatusId(AccountStatus.ACTIVE);
        acc1.setUser(customer1);
        accountRepository.save(acc1);

        User customer2 = new User();
        customer2.setFirstName("Laura");
        customer2.setLastName("Bianchi");
        customer2.setBirthDate(java.time.LocalDate.of(1990, 7, 22));
        customer2.setEmail("laura.bianchi@example.com");
        customer2.setUsername("laura.bianchi@example.com");
        customer2.setPassword(hashedPass);
        customer2.setRoleType(roleC);
        customer2.setStatus(activeStatus);
        customer2 = userRepository.save(customer2);
        Account acc2a = new Account();
        acc2a.setAccountNumber("IT" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase());
        acc2a.setBalance(new BigDecimal("3000.00"));
        acc2a.setStatusId(AccountStatus.ACTIVE);
        acc2a.setUser(customer2);
        accountRepository.save(acc2a);
        Account acc2b = new Account();
        acc2b.setAccountNumber("IT" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase());
        acc2b.setBalance(new BigDecimal("750.50"));
        acc2b.setStatusId(AccountStatus.ACTIVE);
        acc2b.setUser(customer2);
        accountRepository.save(acc2b);

        User employee = new User();
        employee.setFirstName("Admin");
        employee.setLastName("Bank");
        employee.setBirthDate(java.time.LocalDate.of(1980, 1, 1));
        employee.setEmail("admin@javaisland.com");
        employee.setUsername("admin");
        employee.setPassword(hashedPass);
        employee.setRoleType(roleD);
        employee.setStatus(activeStatus);
        userRepository.save(employee);
    }
}
