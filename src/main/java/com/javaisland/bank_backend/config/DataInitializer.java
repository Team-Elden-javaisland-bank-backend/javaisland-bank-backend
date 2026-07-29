package com.javaisland.bank_backend.config;

import com.javaisland.bank_backend.account.model.AccountStatus;
import com.javaisland.bank_backend.account.repository.AccountStatusRepository;
import com.javaisland.bank_backend.account.model.LimitType;
import com.javaisland.bank_backend.account.repository.LimitTypeRepository;
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
import com.javaisland.bank_backend.user.repository.UserRepository;
import com.javaisland.bank_backend.user.model.UserStatus;
import com.javaisland.bank_backend.user.repository.UserStatusRepository;
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
        userStatusRepository.save(new UserStatus(null, "ANNULLED"));
        userStatusRepository.save(new UserStatus(null, "SUSPENDED"));
    }

    private void seedRoleTypes() {
        if (roleTypeRepository.count() == 0) {
            roleTypeRepository.save(new RoleType(null, "C"));
            roleTypeRepository.save(new RoleType(null, "D"));
            roleTypeRepository.save(new RoleType(null, "A"));
        } else {
            roleTypeRepository.findByRoleName("CUSTOMER").ifPresent(r -> { r.setRoleName("C"); roleTypeRepository.save(r); });
            roleTypeRepository.findByRoleName("EMPLOYEE").ifPresent(r -> { r.setRoleName("D"); roleTypeRepository.save(r); });
            if (roleTypeRepository.findByRoleName("A").isEmpty()) {
                roleTypeRepository.save(new RoleType(null, "A"));
            }
        }
    }

    private void seedCardStatuses() {
        if (cardStatusRepository.count() == 0) {
            cardStatusRepository.save(new CardStatus(null, "INACTIVE"));
            cardStatusRepository.save(new CardStatus(null, "ACTIVE"));
            cardStatusRepository.save(new CardStatus(null, "BLOCKED"));
        }
        if (cardStatusRepository.findByStatusName("CLOSED").isEmpty()) {
            cardStatusRepository.save(new CardStatus(null, "CLOSED"));
        }
    }

    private void seedCardTypes() {
        if (cardTypeRepository.count() > 0) return;
        cardTypeRepository.save(new CardType(null, "DEBIT"));
    }

    private void seedAccountStatuses() {
        if (accountStatusRepository.count() == 0) {
            accountStatusRepository.save(new AccountStatus(null, "INACTIVE"));
            accountStatusRepository.save(new AccountStatus(null, "ACTIVE"));
            accountStatusRepository.save(new AccountStatus(null, "FROZEN"));
            accountStatusRepository.save(new AccountStatus(null, "CLOSED"));
        }
    }

    private void seedLimitTypes() {
        if (limitTypeRepository.count() > 0) return;
        limitTypeRepository.save(new LimitType(null, "DAILY_TRANSFER", "Importo massimo cumulativo di bonifico giornaliero", LimitType.ChangePolicy.USER_LOWER_ONLY));
        limitTypeRepository.save(new LimitType(null, "SINGLE_TRANSFER", "Importo massimo per singolo bonifico", LimitType.ChangePolicy.USER_LOWER_ONLY));
        limitTypeRepository.save(new LimitType(null, "INSTANT_TRANSFER_SINGLE", "Importo massimo per singolo bonifico istantaneo", LimitType.ChangePolicy.BANK_ONLY));
        limitTypeRepository.save(new LimitType(null, "MONTHLY_TRANSFER", "Importo massimo cumulativo di bonifico mensile", LimitType.ChangePolicy.BANK_ONLY));
        limitTypeRepository.save(new LimitType(null, "ATM_WITHDRAWAL", "Prelievo massimo al bancomat per transazione", LimitType.ChangePolicy.USER_FULL));
        limitTypeRepository.save(new LimitType(null, "POS_SPENDING", "Spesa massima POS per transazione", LimitType.ChangePolicy.USER_FULL));
    }

    private void seedTransactionTypes() {
        seedIfMissing(transactionTypeRepository, "DEPOSIT");
        seedIfMissing(transactionTypeRepository, "WITHDRAWAL");
        seedIfMissing(transactionTypeRepository, "TRANSFER");
        seedIfMissing(transactionTypeRepository, "INITIAL_TRANSFER");
        seedIfMissing(transactionTypeRepository, "INSTANT_TRANSFER");
    }

    private void seedIfMissing(TransactionTypeRepository repo, String name) {
        if (repo.findByTypeName(name).isEmpty()) {
            repo.save(new TransactionType(null, name));
        }
    }

    private void seedTransactionStatuses() {
        seedIfMissing(transactionStatusRepository, "PENDING");
        seedIfMissing(transactionStatusRepository, "COMPLETED");
        seedIfMissing(transactionStatusRepository, "FAILED");
        seedIfMissing(transactionStatusRepository, "REJECTED");
        seedIfMissing(transactionStatusRepository, "CANCELLED");
    }

    private void seedIfMissing(TransactionStatusRepository repo, String name) {
        if (repo.findByStatusName(name).isEmpty()) {
            repo.save(new TransactionStatus(null, name));
        }
    }
}
