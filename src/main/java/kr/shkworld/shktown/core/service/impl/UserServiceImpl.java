package kr.shkworld.shktown.core.service.impl;

import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.repository.UserRepository;
import kr.shkworld.shktown.core.service.UserService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final Map<UUID, User> onlineUsers = new ConcurrentHashMap<>();

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public CompletableFuture<Optional<User>> getUserAsync(UUID uuid) {
        if (onlineUsers.containsKey(uuid)) {
            return CompletableFuture.completedFuture(Optional.of(onlineUsers.get(uuid)));
        }

        return CompletableFuture.supplyAsync(() -> {
            Optional<User> user = userRepository.loadUserFromDB(uuid);
            user.ifPresent(u -> onlineUsers.put(uuid, u));
            return user;
        });
    }

    @Override
    public Collection<User> getOnlineUsers() {
        return onlineUsers.values();
    }

    @Override
    public CompletableFuture<Optional<User>> getUserByAccountNumber(String accountNumber) {
        return userRepository.findUUIDByAccountNumber(accountNumber)
                .thenCompose(uuidOpt -> {
                    if (uuidOpt.isPresent()) {
                        return getUserAsync(uuidOpt.get());
                    }
                    return CompletableFuture.completedFuture(Optional.empty());
                });
    }

    @Override
    public void saveUser(User user) {
        onlineUsers.put(user.getUUID(), user);
        userRepository.saveToDB(user);
    }

    @Override
    public void unloadUser(UUID uuid) {
        onlineUsers.remove(uuid);
    }
}
