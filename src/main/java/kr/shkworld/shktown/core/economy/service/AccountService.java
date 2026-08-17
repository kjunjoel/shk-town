package kr.shkworld.shktown.core.economy.service;

import kr.shkworld.shktown.core.economy.model.Account;
import kr.shkworld.shktown.core.economy.model.AccountType;
import kr.shkworld.shktown.core.economy.model.TransactionReason;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AccountService {

    Optional<Account> getAccountSync(String accountNumber);

    CompletableFuture<Optional<Account>> getAccountAsync(String accountNumber);

    CompletableFuture<Optional<Account>> refreshAccountAsync(String accountNumber);

    Optional<Account> getAccountByOwnerSync(UUID ownerUuid, AccountType accountType);

    CompletableFuture<Optional<Account>> getAccountByOwnerAsync(UUID ownerUuid, AccountType accountType);

    CompletableFuture<Account> createAccount(UUID ownerUuid, AccountType accountType);

    CompletableFuture<Account> ensureAccountAsync(UUID ownerUuid, AccountType accountType);

    CompletableFuture<Void> deleteAccount(String accountNumber);

    CompletableFuture<Void> flushAndUnloadOwnerAccountsAsync(UUID ownerUuid);

    void flushAllSync();

    void depositSync(String accountNumber, BigDecimal amount, TransactionReason reason, String detail);

    CompletableFuture<Void> depositAsync(String accountNumber, BigDecimal amount, TransactionReason reason, String detail);

    boolean withdrawSync(String accountNumber, BigDecimal amount, TransactionReason reason, String detail);

    CompletableFuture<Boolean> withdrawAsync(String accountNumber, BigDecimal amount, TransactionReason reason, String detail);
}
