package kr.shkworld.shktown.listener;

import org.bukkit.plugin.PluginManager;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.chat.ChatManager;
import kr.shkworld.shktown.integration.worldguard.WorldGuardHook;

public class EventManager {
    private final SHKTown plugin;

    public EventManager(SHKTown plugin) {
        this.plugin = plugin;
    }

    public void registerEvents(WorldGuardHook worldGuardHook, ChatManager chatManager) {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new PlayerJoinListener(
            plugin.getUserService(),
            plugin.getAccountService(),
            plugin.getLogService()
        ), plugin);
        pm.registerEvents(new ChatListener(chatManager), plugin);
        pm.registerEvents(new RegionAreaListener(worldGuardHook, chatManager, plugin), plugin);
    }
}
