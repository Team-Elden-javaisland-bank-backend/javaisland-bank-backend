package com.javaisland.bank_backend.account;

/**
 * Mirrors the seeded rows of the `account_statuses` lookup table.
 * Keep these IDs in sync with the DB seed data / migration scripts.
 */
public final class AccountStatus {
    public static final int INACTIVE = 1; // created, waiting for employee validation
    public static final int ACTIVE = 2;   // usable account
    public static final int FROZEN = 3;   // used here also as "closure requested, pending employee validation"
    public static final int CLOSED = 4;   // closure validated by an employee, no longer usable

    private AccountStatus() {
    }
}
