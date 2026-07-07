package com.javaisland.bank_backend.transaction;

/** Mirrors the seeded rows of the `transaction_types` lookup table. */
public final class TransactionType {
    public static final int DEPOSIT = 1;
    public static final int WITHDRAWAL = 2;
    public static final int TRANSFER = 3;
    public static final int INITIAL_TRANSFER = 4;

    private TransactionType() {
    }
}
