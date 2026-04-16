package kr.shkworld.shktown.core.service;

import kr.shkworld.shktown.core.model.User;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserService {
    CompletableFuture<Optional<User>> getUserAsync(UUID uuid);
    Collection<User> getOnlineUsers();
    CompletableFuture<Optional<User>> getUserByAccountNumber(String accountNumber);
    void saveUser(User user);
    void unloadUser(UUID uuid);
}