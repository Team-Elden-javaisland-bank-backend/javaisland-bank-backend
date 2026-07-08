package com.javaisland.bank_backend.config;

import com.javaisland.bank_backend.account.AccountStatus;
import com.javaisland.bank_backend.account.AccountStatusRepository;
import com.javaisland.bank_backend.account.LimitType;
import com.javaisland.bank_backend.account.LimitTypeRepository;
import com.javaisland.bank_backend.card.CardStatus;
import com.javaisland.bank_backend.card.CardStatusRepository;
import com.javaisland.bank_backend.card.CardType;
import com.javaisland.bank_backend.card.CardTypeRepository;
import com.javaisland.bank_backend.transaction.TransactionStatus;
import com.javaisland.bank_backend.transaction.TransactionStatusRepository;
import com.javaisland.bank_backend.transaction.TransactionType;
import com.javaisland.bank_backend.transaction.TransactionTypeRepository;
import com.javaisland.bank_backend.user.RoleType;
import com.javaisland.bank_backend.user.RoleTypeRepository;
import com.javaisland.bank_backend.user.UserStatus;
import com.javaisland.bank_backend.user.UserStatusRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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

    public DataInitializer(UserStatusRepository userStatusRepository,
                           RoleTypeRepository roleTypeRepository,
                           CardStatusRepository cardStatusRepository,
                           CardTypeRepository cardTypeRepository,
                           AccountStatusRepository accountStatusRepository,
                           LimitTypeRepository limitTypeRepository,
                           TransactionTypeRepository transactionTypeRepository,
                           TransactionStatusRepository transactionStatusRepository) {
        this.userStatusRepository = userStatusRepository;
        this.roleTypeRepository = roleTypeRepository;
        this.cardStatusRepository = cardStatusRepository;
        this.cardTypeRepository = cardTypeRepository;
        this.accountStatusRepository = accountStatusRepository;
        this.limitTypeRepository = limitTypeRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.transactionStatusRepository = transactionStatusRepository;
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
        userStatusRepository.save(new UserStatus(null, "ANNULED"));
        userStatusRepository.save(new UserStatus(null, "SUSPENDED"));
    }

    private void seedRoleTypes() {
        if (roleTypeRepository.count() > 0) return;
        roleTypeRepository.save(new RoleType(null, "CUSTOMER"));
        roleTypeRepository.save(new RoleType(null, "EMPLOYEE"));
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
}
