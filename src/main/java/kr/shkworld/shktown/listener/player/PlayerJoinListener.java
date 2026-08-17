package kr.shkworld.shktown.listener.player;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.core.economy.model.AccountType;
import kr.shkworld.shktown.core.economy.model.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerJoinListener implements Listener {
    private final SHKTown plugin;
    private final Map<UUID, User> pendingUsers = new ConcurrentHashMap<>();

    public PlayerJoinListener(SHKTown plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        try {
            User user = plugin.getUserService().prepareUserAsync(uuid, event.getName()).join();
            pendingUsers.put(uuid, user);
        } catch (CompletionException e) {
            plugin.getLogger().severe("로그인 사용자 DB 조회 실패: " + uuid + " - " + rootMessage(e));
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    "사용자 데이터를 불러오지 못했습니다. 잠시 후 다시 접속해 주세요."
            );
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();
        pendingUsers.remove(uuid);

        CompletableFuture<Void> userFuture = plugin.getUserService().updateLoginAsync(uuid, playerName);

        userFuture.thenCompose(ignored -> {
            CompletableFuture<?> savings = plugin.getAccountService().ensureAccountAsync(uuid, AccountType.SAVINGS);
            CompletableFuture<?> investment = plugin.getAccountService().ensureAccountAsync(uuid, AccountType.INVESTMENT);
            return CompletableFuture.allOf(savings, investment);
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("접속 사용자 데이터 준비 실패: " + uuid + " - " + rootMessage(throwable));
            return null;
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingUsers.remove(uuid);

        CompletableFuture<Void> accountFlush = plugin.getAccountService()
                .flushAndUnloadOwnerAccountsAsync(uuid)
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger().severe("퇴장 계좌 데이터 저장 실패: "
                                + uuid + " - " + rootMessage(throwable));
                    }
                });

        CompletableFuture<Void> userFlush = plugin.getUserService()
                .flushAndUnloadUserAsync(uuid)
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger().severe("퇴장 사용자 데이터 저장 실패: "
                                + uuid + " - " + rootMessage(throwable));
                    }
                });

        CompletableFuture.allOf(userFlush, accountFlush);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage();
    }
}
