package kr.shkworld.shktown.core.economy.repository;

import kr.shkworld.shktown.core.economy.model.User;
import kr.shkworld.shktown.core.economy.model.CashReason;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserRepository {
    
    void saveUserSync(User user) throws SQLException;

    CompletableFuture<Void> saveUserAsync(User user);

    Optional<User> loadUserSync(UUID uuid) throws SQLException;

    CompletableFuture<Optional<User>> loadUserAsync(UUID uuid);

    Optional<User> addCashSync(UUID uuid, BigDecimal amount, CashReason reason, String detail) throws SQLException;

    CompletableFuture<Optional<User>> addCashAsync(UUID uuid, BigDecimal amount, CashReason reason, String detail);

    Optional<User> subtractCashSync(UUID uuid, BigDecimal amount, CashReason reason, String detail) throws SQLException;

    CompletableFuture<Optional<User>> subtractCashAsync(UUID uuid, BigDecimal amount, CashReason reason, String detail);
}
