package kr.shkworld.shktown.core.economy.service.impl;

import kr.shkworld.shktown.core.economy.model.CashReason;
import kr.shkworld.shktown.core.logging.model.LogType;
import kr.shkworld.shktown.core.economy.model.User;
import kr.shkworld.shktown.core.economy.repository.UserRepository;
import kr.shkworld.shktown.core.logging.service.LogService;
import kr.shkworld.shktown.core.economy.service.UserService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final LogService logService;

    private final Map<UUID, User> onlineUsers = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Void>> cashOperationQueues = new ConcurrentHashMap<>();

    public UserServiceImpl(UserRepository userRepository, LogService logService) {
        this.userRepository = userRepository;
        this.logService = logService;
    }

    @Override
    public Optional<User> getUserSync(UUID uuid) {
        return Optional.ofNullable(onlineUsers.get(uuid));
    }

    @Override
    public CompletableFuture<Optional<User>> getUserAsync(UUID uuid) {
        User cached = onlineUsers.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return userRepository.loadUserAsync(uuid)
                .thenApply(userOpt -> {
                    userOpt.ifPresent(this::loadUser);
                    return userOpt;
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        logService.logSystem(
                            LogType.ERROR,
                            "User 비동기 불러오기 중 DB 예외 발생",
                            Map.of("uuid", uuid.toString(), "error", throwable.getMessage())
                        );
                    }
                });
    }

    private void loadUser(User user) {
        onlineUsers.put(user.getUuid(), user);
    }

    private void unloadUser(UUID uuid) {
        onlineUsers.remove(uuid);
    }

    private void saveUserSync(User user) {
        try {
            userRepository.saveUserSync(user);
        } catch (Exception e) {
            logService.logSystem(
                LogType.ERROR,
                "User 동기 저장 중 DB 예외 발생",
                Map.of("uuid", user.getUuid().toString(), "error", e.getMessage())
            );
        }
    }

    private CompletableFuture<Void> saveUserAsync(User user) {
        return userRepository.saveUserAsync(user)
        .whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                logService.logSystem(
                    LogType.ERROR,
                    "User 비동기 저장 중 DB 예외 발생",
                    Map.of("uuid", user.getUuid().toString(), "error", throwable.getMessage())
                );
            }
        });
    }

    @Override
    public CompletableFuture<User> prepareUserAsync(UUID uuid, String name) {
        return getUserAsync(uuid).thenApply(userOpt -> {
            User user = userOpt.orElseGet(() -> new User(uuid, name));
            loadUser(user);
            return user;
        });
    }

    @Override
    public CompletableFuture<Void> updateLoginAsync(UUID uuid, String name) {
        return getUserAsync(uuid).thenCompose(userOpt -> {
            User user = userOpt.orElseGet(() -> new User(uuid, name));
            user.setName(name);
            user.setLastLogin(java.time.LocalDateTime.now());
            loadUser(user);
            return saveUserAsync(user);
        });
    }

    @Override
    public CompletableFuture<Void> flushAndUnloadUserAsync(UUID uuid) {
        User user = onlineUsers.get(uuid);
        if (user == null) {
            return CompletableFuture.completedFuture(null);
        }
        return saveUserAsync(user).thenRun(() -> unloadUser(uuid));
    }

    @Override
    public void flushAllSync() {
        onlineUsers.values().forEach(this::saveUserSync);
    }

    @Override
    public void updateNameSync(UUID uuid, String name) {
        Optional<User> userOpt = getUserSync(uuid);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();
        user.setName(name);
        saveUserSync(user);
    }

    @Override
    public CompletableFuture<Void> updateNameAsync(UUID uuid, String name) {
        return getUserAsync(uuid).thenCompose(userOpt -> {
            if (userOpt.isEmpty()) return CompletableFuture.completedFuture(null);

            User user = userOpt.get();
            user.setName(name);
            return saveUserAsync(user);
        });
    }

    @Override
    public void addCashSync(UUID uuid, BigDecimal amount, CashReason reason, String detail) {
        try {
            addCashAsync(uuid, amount, reason, detail).join();
        } catch (CompletionException ignored) {
        }
    }

    @Override
    public CompletableFuture<Void> addCashAsync(UUID uuid, BigDecimal amount, CashReason reason, String detail) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        return enqueueCashOperation(uuid, () ->
                userRepository.addCashAsync(uuid, amount, reason, detail)
                        .thenAccept(userOpt -> userOpt.ifPresent(this::loadUser)))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        logService.logSystem(
                            LogType.ERROR,
                            "Cash 비동기 지급 중 DB 예외 발생",
                            Map.of("uuid", uuid.toString(), "error", throwable.getMessage())
                        );
                    }
                });
    }

    @Override
    public boolean subtractCashSync(UUID uuid, BigDecimal amount, CashReason reason, String detail) {
        try {
            return subtractCashAsync(uuid, amount, reason, detail).join();
        } catch (CompletionException e) {
            logService.logSystem(
                LogType.ERROR,
                "Cash 동기 차감 중 DB 예외 발생",
                Map.of("uuid", uuid.toString(), "error", e.getMessage())
            );
            return false;
        }
    }

    @Override
    public CompletableFuture<Boolean> subtractCashAsync(UUID uuid, BigDecimal amount, CashReason reason, String detail) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.completedFuture(false);
        }

        return enqueueCashOperation(uuid, () ->
                userRepository.subtractCashAsync(uuid, amount, reason, detail)
                        .thenApply(userOpt -> {
                            userOpt.ifPresent(this::loadUser);
                            return userOpt.isPresent();
                        }))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        logService.logSystem(
                            LogType.ERROR,
                            "Cash 비동기 차감 중 DB 예외 발생",
                            Map.of("uuid", uuid.toString(), "error", throwable.getMessage())
                        );
                    }
                });
    }

    private <T> CompletableFuture<T> enqueueCashOperation(
            UUID uuid,
            Supplier<CompletableFuture<T>> operation
    ) {
        CompletableFuture<T> next;
        CompletableFuture<Void> marker;
        synchronized (cashOperationQueues) {
            CompletableFuture<Void> previous = cashOperationQueues.getOrDefault(
                    uuid,
                    CompletableFuture.completedFuture(null)
            );

            next = previous
                    .handle((ignored, throwable) -> null)
                    .thenCompose(ignored -> operation.get());
            marker = next.handle((ignored, throwable) -> null);

            cashOperationQueues.put(uuid, marker);
        }
        marker.whenComplete((ignored, throwable) -> cashOperationQueues.remove(uuid, marker));

        return next;
    }
}
