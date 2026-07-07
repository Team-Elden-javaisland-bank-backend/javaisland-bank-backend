package com.javaisland.bank_backend.transaction;

/** Mirrors the seeded rows of the `transaction_statuses` lookup table. */
public final class TransactionStatus {
    public static final int PENDING = 1;
    public static final int COMPLETED = 2;
    public static final int FAILED = 3;
    public static final int REJECTED = 4;

    private TransactionStatus() {
    }
}
