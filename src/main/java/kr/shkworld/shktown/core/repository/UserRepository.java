package kr.shkworld.shktown.core.repository;

import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.RankEntry;
import kr.shkworld.shktown.core.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {
    Optional<User> loadUserFromDB(UUID uuid);
    void saveToDB(User user);
    CompletableFuture<Optional<UUID>> findUUIDByAccountNumber(String accountNumber);
    CompletableFuture<List<RankEntry>> getTopRanksAsync(AccountType type, int page);
}
