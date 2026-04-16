package kr.shkworld.shktown.listener;

import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.LogType;
import kr.shkworld.shktown.core.model.User;
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

public class PlayerJoinListener implements Listener {
    private final UserService userService;
    private final LogService logService;

    public PlayerJoinListener(UserService userService, LogService logService) {
        this.userService = userService;
        this.logService = logService;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();

        userService.getUserAsync(uuid).thenAccept(userOpt -> {
            if (userOpt.isEmpty()) {
                User newUser = new User(uuid, name);
                newUser.addAccount(AccountType.PERSONAL);
                newUser.addAccount(AccountType.CASH);
                newUser.addAccount(AccountType.STOCK);
                newUser.addAccount(AccountType.CRYPTO);

                userService.saveUser(newUser);
            }
        }).join();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = (player.getAddress() != null) ? player.getAddress().getHostString() : "UNKNOWN";

        logService.log(LogType.ACCESS, uuid.toString(), "JOIN", Map.of(
                "name", player.getName(),
                "ip", ip
        ));

        userService.getUserAsync(uuid).thenAccept(userOpt -> {
            userOpt.ifPresent(user -> {
                player.sendMessage("§a서버에 다시 오신 것을 환영합니다, " + player.getName() + "님!");
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String reason = event.getReason().toString();

        logService.log(LogType.ACCESS, uuid.toString(), "QUIT", Map.of(
                "name", player.getName(),
                "reason", reason
        ));

        userService.unloadUser(uuid);
    }
}
