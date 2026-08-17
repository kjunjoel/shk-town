package kr.shkworld.shktown.core.economy.repository;

import kr.shkworld.shktown.core.economy.model.Account;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AccountRepository {
    
    void saveAccountSync(Account account) throws SQLException;

    CompletableFuture<Void> saveAccountAsync(Account account);

    Optional<Account> loadAccountByNumberSync(String accountNumber) throws SQLException;

    CompletableFuture<Optional<Account>> loadAccountByNumberAsync(String accountNumber);

    List<Account> loadAccountByOwnerSync(UUID ownerUuid) throws SQLException;

    CompletableFuture<List<Account>> loadAccountByOwnerAsync(UUID ownerUuid);

    CompletableFuture<Void> deleteAccount(String accountNumber);
}
