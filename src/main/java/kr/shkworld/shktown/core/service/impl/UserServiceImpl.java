package kr.shkworld.shktown.core.service.impl;

import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.repository.AccountRepository;
import kr.shkworld.shktown.core.repository.UserRepository;
import kr.shkworld.shktown.core.service.UserService;
import kr.shkworld.util.PluginLogger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PluginLogger pluginLogger;

    private final Map<UUID, User> onlineUsers = new ConcurrentHashMap<>();

    public UserServiceImpl(UserRepository userRepository, AccountRepository accountRepository, PluginLogger pluginLogger) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.pluginLogger = pluginLogger;
    }

    @Override
    public User getUserFromCache(UUID uuid) {
        return onlineUsers.get(uuid);
    }

    @Override
    public CompletableFuture<Optional<User>> getUserAsync(UUID uuid) {
        if (onlineUsers.containsKey(uuid)) {
            return CompletableFuture.completedFuture(Optional.of(onlineUsers.get(uuid)));
        }

        CompletableFuture<Optional<User>> userFuture = userRepository.loadUser(uuid);
        CompletableFuture<List<Account>> accountsFuture = accountRepository.findAllByOwner(uuid);

        return userFuture.thenCombine(accountsFuture, (userOpt, accounts) -> {
            userOpt.ifPresent(user -> {
                accounts.forEach(user::addAccount);
                loadUser(user);
            });
            return userOpt;
        }).exceptionally(throwable -> {
            pluginLogger.severe("유저를 불러오던 중 오류가 발생하였습니다.\n" + throwable.getCause());
            return Optional.empty();
        });
    }

    @Override
    public void updateName(UUID uuid, String name) {
        if (!onlineUsers.containsKey(uuid)) return;

        User user = onlineUsers.get(uuid);
        user.setName(name);
        saveUser(user);
    }

    @Override
    public CompletableFuture<Void> updateAffiliation(UUID uuid, long townID, long nationID) {
        return getUserAsync(uuid).thenCompose(optUser -> {
            if (optUser.isEmpty()) {
                pluginLogger.warning("존재하지 않는 유저의 소속을 업데이트 할 수 없습니다: " + uuid);
                return CompletableFuture.completedFuture(null);
            }

            User user = optUser.get();
            user.setTownID(townID);
            user.setNationID(nationID);
            return saveUser(user);
        });
    }

    @Override
    public CompletableFuture<Void> saveUser(User user) {
        return userRepository.saveUser(user);
    }

    @Override
    public void saveAllSync() {
        pluginLogger.info("모든 유저 데이터를 DB에 저장합니다.");
        getOnlineUsers().values().forEach(user -> {
            userRepository.saveUserSync(user);
            user.getAccounts().values().forEach(accountRepository::saveAccountSync);
        });
    }

    @Override
    public void loadUser(User user) {
        onlineUsers.put(user.getUUID(), user);
    }

    @Override
    public void unloadUser(UUID uuid) {
        onlineUsers.remove(uuid);
    }

    @Override
    public Map<UUID, User> getOnlineUsers() {
        return onlineUsers;
    }

    @Override
    public CompletableFuture<List<UUID>> findTownMembersByID(long townID) {
        return userRepository.findUUIDsByTownID(townID);
    }

    @Override
    public CompletableFuture<List<UUID>> findNationMembersByID(long nationID) {
        return userRepository.findUUIDsByNationID(nationID);
    }
}
