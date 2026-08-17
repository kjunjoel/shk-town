package kr.shkworld.shktown.listener;

import kr.shkworld.shktown.listener.player.PlayerJoinListener;
import kr.shkworld.shktown.listener.player.PlayerQuitListener;
import kr.shkworld.shktown.listener.shop.ShopClickListener;
import org.bukkit.plugin.PluginManager;

import kr.shkworld.shktown.SHKTown;

public class EventManager {
    private final SHKTown plugin;

    public EventManager(SHKTown plugin) {
        this.plugin = plugin;
    }

    public void registerEvents() {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new PlayerJoinListener(plugin), plugin);
        pm.registerEvents(new PlayerQuitListener(plugin), plugin);
        pm.registerEvents(new ShopClickListener(), plugin);
    }
}
