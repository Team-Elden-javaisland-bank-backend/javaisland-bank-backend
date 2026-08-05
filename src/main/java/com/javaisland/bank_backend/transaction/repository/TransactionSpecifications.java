package com.javaisland.bank_backend.transaction.repository;

import com.javaisland.bank_backend.transaction.model.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Collection;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> forAccountIds(Collection<Long> accountIds) {
        return (root, query, cb) -> cb.or(
                root.get("sourceAccount").get("id").in(accountIds),
                root.get("destinationAccount").get("id").in(accountIds)
        );
    }

    public static Specification<Transaction> createdBetween(OffsetDateTime start, OffsetDateTime end) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), start, end);
    }
}
