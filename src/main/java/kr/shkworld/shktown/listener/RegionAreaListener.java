package kr.shkworld.shktown.listener;

import java.time.Duration;
import java.util.Optional;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import kr.shkworld.shktown.chat.ChatManager;
import kr.shkworld.shktown.integration.worldguard.WorldGuardHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;

public class RegionAreaListener implements Listener {
    private final WorldGuardHook wgHook;
    private final ChatManager chatManager;
    private final JavaPlugin plugin;

    public RegionAreaListener(WorldGuardHook wgHook, ChatManager chatManager, JavaPlugin plugin) {
        this.wgHook = wgHook;
        this.chatManager = chatManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        Optional<String> regionIDOpt = wgHook.getCurrentRegionId(player);
        String previousRegion = chatManager.getPlayerRegionDisplayName(player);
        FileConfiguration config = plugin.getConfig();

        if (regionIDOpt.isPresent()) {
            String regionID = regionIDOpt.get();
            String currentDisplayName = getRegionDisplayNameFromConfig(config, regionID);

            if (!currentDisplayName.equals(previousRegion)) {
                chatManager.setPlayerRegion(player.getUniqueId(), currentDisplayName);
                String titleStr = config.getString("chat.message.entry-title", "")
                        .replace("%region%", currentDisplayName);
                String subtitleStr = config.getString("chat.message.entry-subtitle", "")
                        .replace("%region%", currentDisplayName);

                Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(titleStr);
                Component subtitleComponent = LegacyComponentSerializer.legacySection().deserialize(subtitleStr);

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(500)
                );

                Title title = Title.title(titleComponent, subtitleComponent, times);
                player.showTitle(title);
            }
        } else {
            if (!previousRegion.equals("전체 채널")) {
                chatManager.removePlayerRegion(player.getUniqueId());
                
                String titleStr = config.getString("chat.message.quit-title", "")
                        .replace("%region%", previousRegion);
                String subtitleStr = config.getString("chat.message.quit-subtitle", "")
                        .replace("%region%", previousRegion);

                Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(titleStr);
                Component subtitleComponent = LegacyComponentSerializer.legacySection().deserialize(subtitleStr);

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(500)
                );

                Title title = Title.title(titleComponent, subtitleComponent, times);
                player.showTitle(title);
            }
        }
    }

    private String getRegionDisplayNameFromConfig(FileConfiguration config, String regionID) {
        String path = "chat.local-regions." + regionID;
        return config.getString(path, regionID);
    }
}
