package kr.shkworld.shktown.core.economy.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Account {
    private final String accountNumber; // 계좌번호
    private final UUID ownerUuid; // 소유주의 UUID
    private final AccountType accountType; // 계좌의 유형
    private final BigDecimal balance; // 현재 잔액

    public Account(String accountNumber, UUID ownerUuid, AccountType accountType, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.ownerUuid = ownerUuid;
        this.accountType = accountType;
        this.balance = balance;
    }

    public String getAccountNumber() { return accountNumber; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public AccountType getAccountType() { return accountType; }
    public BigDecimal getBalance() { return balance; }
}
