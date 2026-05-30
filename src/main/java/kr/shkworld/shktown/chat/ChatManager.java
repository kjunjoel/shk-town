package kr.shkworld.shktown.chat;

import kr.shkworld.shktown.integration.luckperms.LuckPermsHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.model.user.User;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {
    private final LuckPermsHook lpHook;

    private final Map<UUID, String> playerCurrentRegion = new ConcurrentHashMap<>();

    public ChatManager(LuckPermsHook lpHook) {
        this.lpHook = lpHook;
    }

    public void setPlayerRegion(UUID uuid, String displayName) {
        playerCurrentRegion.put(uuid, displayName);
    }

    public void removePlayerRegion(UUID uuid) {
        playerCurrentRegion.remove(uuid);
    }

    public String getPlayerRegionDisplayName(Player player) {
        return playerCurrentRegion.getOrDefault(player.getUniqueId(), "전체 채팅");
    }

    public void processChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        Component formattedContent = formatMessage(sender, event.message());
        String currentRegionDisplayName = getPlayerRegionDisplayName(sender);

        if (!currentRegionDisplayName.equals("전체 채팅")) {
            Component prefix = LegacyComponentSerializer.legacySection().deserialize("§a[ "+ currentRegionDisplayName + "] §f");
            Component finalMessage = prefix.append(formattedContent);
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> getPlayerRegionDisplayName(p).equals(currentRegionDisplayName))
                    .forEach(recipient -> recipient.sendMessage(finalMessage));

        } else {
            Component prefix = LegacyComponentSerializer.legacySection().deserialize("§b[전체] §f");
            Bukkit.broadcast(prefix.append(formattedContent));
        }
    }

    private Component formatMessage(Player player, Component rawMessage) {
        String prefix = "";

        if (lpHook.getAPI().isPresent()) {
            User user = lpHook.getAPI().get().getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                if (lpPrefix != null) {
                    prefix = lpPrefix;
                }
            }
        }

        Component prefixComponent = LegacyComponentSerializer.legacySection().deserialize(prefix);
        Component playerComponent = Component.text(player.getName());
        Component separator = Component.text(" : ");

        return prefixComponent.append(playerComponent).append(separator).append(rawMessage);
    }
}