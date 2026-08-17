package kr.shkworld.shktown.listener.player;

import kr.shkworld.shktown.SHKTown;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {
    private final SHKTown plugin;

    public PlayerQuitListener(SHKTown plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getNavigationManager().isNavigating(event.getPlayer())) {
            plugin.getNavigationManager().stopNavigation(event.getPlayer(), false);
        }
    }
}
