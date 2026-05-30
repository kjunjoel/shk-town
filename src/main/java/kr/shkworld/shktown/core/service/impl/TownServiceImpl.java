package kr.shkworld.shktown.core.service.impl;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.Town;
import kr.shkworld.shktown.core.repository.TownRepository;
import kr.shkworld.shktown.core.service.AccountService;
import kr.shkworld.shktown.core.service.UserService;
import kr.shkworld.util.PluginLogger;
import kr.shkworld.shktown.core.service.TownService;

public class TownServiceImpl implements TownService {
    private final UserService userService;
    private final AccountService accountService;
    private final TownRepository townRepository;
    private final PluginLogger pluginLogger;

    public TownServiceImpl(UserService userService, AccountService accountService, TownRepository townRepository, PluginLogger pluginLogger) {
        this.userService = userService;
        this.accountService = accountService;
        this.townRepository = townRepository;
        this.pluginLogger = pluginLogger;
    }

    @Override
    public CompletableFuture<Boolean> createTown(String name, UUID mayor) {
        UUID townUUID = UUID.nameUUIDFromBytes(("TOWN:" + name + ":" + System.currentTimeMillis()).getBytes());
        Town newTown = new Town(-1L, townUUID, name, mayor, -1L);

        return townRepository.saveTown(newTown)
            .thenCompose(savedTown -> 
                userService.updateAffiliation(mayor, savedTown.getID(), -1L)
                .thenCompose(v -> 
                    accountService.createAccount(townUUID, AccountType.TOWN)
                )
                .thenApply(v -> true)
            )
            .exceptionally(ex -> {
                pluginLogger.warning("마을 생성 실패: " + name + " (" + ex.getMessage() + ")");
                return false;
            });
    }

    @Override
    public CompletableFuture<Optional<Town>> getTown(long id) {
        return townRepository.findByID(id);
    }

    @Override
    public CompletableFuture<Optional<Town>> getTown(UUID uuid) {
        return townRepository.findByUUID(uuid.toString());
    }

    @Override
    public CompletableFuture<Optional<Town>> getTown(String name) {
        return townRepository.findByName(name);
    }

    @Override
    public CompletableFuture<Optional<Town>> getTownWithMembers(long id) {
        return getTown(id).thenCompose(this::assembleMembers);
    }

    @Override
    public CompletableFuture<Optional<Town>> getTownWithMembers(UUID uuid) {
        return getTown(uuid).thenCompose(this::assembleMembers);
    }

    @Override
    public CompletableFuture<Optional<Town>> getTownWithMembers(String name) {
        return getTown(name).thenCompose(this::assembleMembers);
    }

    private CompletableFuture<Optional<Town>> assembleMembers(Optional<Town> optTown) {
        if (optTown.isEmpty()) return CompletableFuture.completedFuture(Optional.empty());
        
        Town town = optTown.get();
        return userService.findTownMembersByID(town.getID()).thenApply(members -> {
            town.setMembers(members);
            return Optional.of(town);
        });
    };
}
