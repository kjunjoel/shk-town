package kr.shkworld.shktown.core.economy.service;

import kr.shkworld.shktown.core.economy.model.CashReason;
import kr.shkworld.shktown.core.economy.model.User;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserService {
    Optional<User> getUserSync(UUID uuid);

    CompletableFuture<Optional<User>> getUserAsync(UUID uuid);

    CompletableFuture<User> prepareUserAsync(UUID uuid, String name);

    CompletableFuture<Void> updateLoginAsync(UUID uuid, String name);

    CompletableFuture<Void> flushAndUnloadUserAsync(UUID uuid);

    void flushAllSync();

    void updateNameSync(UUID uuid, String name);

    CompletableFuture<Void> updateNameAsync(UUID uuid, String name);

    void addCashSync(UUID uuid, BigDecimal amount, CashReason reason, String detail);

    CompletableFuture<Void> addCashAsync(UUID uuid, BigDecimal amount, CashReason reason, String detail);

    boolean subtractCashSync(UUID uuid, BigDecimal amount, CashReason reason, String detail);

    CompletableFuture<Boolean> subtractCashAsync(UUID uuid, BigDecimal amount, CashReason reason, String detail);
}
