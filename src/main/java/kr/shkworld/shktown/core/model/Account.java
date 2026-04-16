package kr.shkworld.shktown.core.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Account {
    private final AccountType accountType;
    private BigDecimal balance;
    private final String accountNumber;
    // private boolean isDirty;

    public Account(AccountType accountType, UUID ownerUUID, int index) {
        this.accountType = accountType;
        this.balance = BigDecimal.ZERO;
        // this.isDirty = true;

        int typeCode = accountType.getCode();
        long timePart = System.currentTimeMillis() % 1_000_000L;
        int userPart = Math.abs(ownerUUID.hashCode() % 1_000);
        this.accountNumber = String.format("%d-%06d-%03d-%02d", typeCode, timePart, userPart, index);
    }

    public Account(AccountType accountType, BigDecimal balance, String accountNumber) {
        this.accountType = accountType;
        this.balance = balance;
        this.accountNumber = accountNumber;
        // this.isDirty = false;
    }

    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        this.balance = this.balance.add(amount);
        // this.isDirty = true;
    }

    public boolean withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (this.balance.compareTo(amount) < 0) return false;
        this.balance = this.balance.subtract(amount);
        // this.isDirty = true;
        return true;
    }

    public AccountType getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
    public String getAccountNumber() { return accountNumber; }
    // public boolean isDirty() { return isDirty; }

    // public void setDirty(boolean dirty) { this.isDirty = dirty; }
}
