package kr.shkworld.shktown.core.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class User {
    private final UUID uuid;
    private String name;
    private long townID;
    private long nationID;

    private final List<Account> accountList = new ArrayList<>();

    public User(UUID uuid, String name) {
        this(uuid, name, -1L, -1L);
    }

    public User(UUID uuid, String name, long townID, long nationID) {
        this.uuid = uuid;
        this.name = name;
        this.townID = townID;
        this.nationID = nationID;
    }

    public BigDecimal getBalance(AccountType accountType) {
        return accountList.stream()
                .filter(acc -> accountType == null || acc.getAccountType() == accountType)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addAccount(AccountType accountType) {
        Account newAccount = new Account(accountType, this.uuid, accountList.size());
        addAccount(newAccount);
    }

    public void addAccount(Account account) {
        if (account == null) return;
        this.accountList.add(account);
    }

    public Optional<Account> getAccountOptional(String accountNumber) {
        return accountList.stream()
                .filter(acc -> acc.getAccountNumber().equals(accountNumber))
                .findFirst();
    }

    public List<Account> getAccountsByType(AccountType accountType) {
        return accountList.stream()
                .filter(acc -> acc.getAccountType() == accountType)
                .toList();
    }

    public UUID getUUID() { return uuid; }
    public String getName() { return name; }
    public long getTownID() { return townID; }
    public long getNationID() { return nationID; }
    public List<Account> getAccountList() { return accountList; }

    public void setName(String name) { this.name = name; }
    public void setTownID(long id) { this.townID = id; }
    public void setNationID(long id) { this.nationID = id; }
}
