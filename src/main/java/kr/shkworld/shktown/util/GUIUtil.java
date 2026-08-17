package kr.shkworld.shktown.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.function.Consumer;

public class GUIUtil {
    private GUIUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    public static ItemStack createItem(Material material, String name, String modelIdentifier, String... loreLines) {
        return buildItem(material, name, modelIdentifier, loreLines, null);
    }

    public static ItemStack createPlayerHead(Player player, String name, String... loreLines) {
        return buildItem(Material.PLAYER_HEAD, name, null, loreLines, meta -> {
            if (meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(player);
            }
        });
    }

    private static ItemStack buildItem(Material material, String name, String modelIdentifier, String[] loreLines, Consumer<ItemMeta> customMetaModifier) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (customMetaModifier != null) {
            customMetaModifier.accept(meta);
        }

        if (name != null && !name.isEmpty()) {
            meta.displayName(TextUtil.parse(name));
        }

        if (loreLines != null && loreLines.length > 0) {
            meta.lore(Arrays.stream(loreLines).map(TextUtil::parse).toList());
        }

        if (modelIdentifier != null && !modelIdentifier.isEmpty()) {
            try {
                int cmd = Integer.parseInt(modelIdentifier);
                if (cmd != 0) {
                    meta.setCustomModelData(cmd);
                }
            } catch (NumberFormatException e) {
                NamespacedKey key = NamespacedKey.fromString(modelIdentifier);
                if (key != null) {
                    meta.setItemModel(key);
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }
}
