package kr.shkworld.shktown.listener;

import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.LogType;
import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.service.AccountService;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.service.UserService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerJoinListener implements Listener {
    private final UserService userService;
    private final AccountService accountService;
    private final LogService logService;

    public PlayerJoinListener(UserService userService, AccountService accountService, LogService logService) {
        this.userService = userService;
        this.accountService = accountService;
        this.logService = logService;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();

        userService.getUserAsync(uuid)
                .thenCompose(userOpt -> {
                    if (userOpt.isPresent()) {
                        return CompletableFuture.completedFuture(userOpt.get());
                    }

                    User newUser = new User(uuid, name);

                    return accountService.createAccount(uuid, AccountType.SAVINGS)
                            .thenCombine(accountService.createAccount(uuid, AccountType.INVESTMENT), (sav, inv) -> {
                                newUser.addAccount(sav);
                                newUser.addAccount(inv);
                                return newUser;
                            })
                            .thenCompose(user -> userService.saveUser(user).thenApply(v -> user));
                })
                .thenAccept(user -> {
                    userService.loadUser(user);
                    userService.updateName(uuid, name);
                })
                .join();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String ip = (player.getAddress() != null) ? player.getAddress().getHostString() : "UNKNOWN";

        logService.logSystem(LogType.ACCESS, "JOIN", Map.of(
                "uuid", uuid,
                "name", name,
                "ip", ip
        ));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String reason = event.getReason().toString();

        User user = userService.getUserFromCache(uuid);
        if (user != null) {
            userService.saveUser(user).thenRun(() -> {
                userService.unloadUser(uuid);
            });
        }

        logService.logSystem(LogType.ACCESS, "QUIT", Map.of(
                "uuid", uuid,
                "name", name,
                "reason", reason
        ));
    }
}
