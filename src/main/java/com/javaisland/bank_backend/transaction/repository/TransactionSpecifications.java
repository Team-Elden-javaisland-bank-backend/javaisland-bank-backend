package com.javaisland.bank_backend.transaction.repository;

import com.javaisland.bank_backend.transaction.model.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
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

    public static Specification<Transaction> createdBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), start, end);
    }

    public static Specification<Transaction> hasType(Integer typeId) {
        return (root, query, cb) -> typeId == null ? null : cb.equal(root.get("typeId"), typeId);
    }

    public static Specification<Transaction> hasStatus(Integer statusId) {
        return (root, query, cb) -> statusId == null ? null : cb.equal(root.get("statusId"), statusId);
    }
}
