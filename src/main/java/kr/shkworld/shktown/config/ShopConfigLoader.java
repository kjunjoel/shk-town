package kr.shkworld.shktown.config;

import kr.shkworld.shktown.core.shop.model.ConfiguredShop;
import kr.shkworld.shktown.core.shop.model.ConfiguredShopItem;
import kr.shkworld.shktown.core.shop.model.ShopAction;
import kr.shkworld.shktown.core.shop.model.ShopCurrency;
import kr.shkworld.shktown.core.shop.model.ShopItemSpec;
import kr.shkworld.shktown.ui.shop.ShopManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public final class ShopConfigLoader {
    private ShopConfigLoader() {
    }

    public static void loadShopConfigs(Map<String, FileConfiguration> configs, ShopManager shopManager) {
        if (configs == null || shopManager == null) {
            return;
        }

        shopManager.clearShops();
        for (Map.Entry<String, FileConfiguration> entry : configs.entrySet()) {
            loadShopConfig(entry.getKey(), entry.getValue(), shopManager);
        }
    }

    private static void loadShopConfig(String fileName, FileConfiguration config, ShopManager shopManager) {
        if (config == null) {
            return;
        }

        ConfigurationSection shopsSection = config.getConfigurationSection("shops");
        if (shopsSection != null) {
            loadLegacyShopConfig(shopsSection, shopManager);
            return;
        }

        String fallbackId = fileName == null ? "shop" : fileName.replaceFirst("\\.ya?ml$", "");
        String shopId = config.getString("id", fallbackId);
        registerShop(shopManager, shopId, config);
    }

    private static void loadLegacyShopConfig(ConfigurationSection shopsSection, ShopManager shopManager) {
        for (String shopId : shopsSection.getKeys(false)) {
            ConfigurationSection shopSection = shopsSection.getConfigurationSection(shopId);
            if (shopSection == null) {
                continue;
            }

            registerShop(shopManager, shopId, shopSection);
        }
    }

    private static void registerShop(ShopManager shopManager, String shopId, ConfigurationSection shopSection) {
        int size = normalizeSize(shopSection.getInt("size", 54));
        Map<Integer, ConfiguredShopItem> items = loadItems(shopSection.getConfigurationSection("items"), size);
        shopManager.registerShop(new ConfiguredShop(
                shopId,
                shopSection.getString("title", shopId),
                size,
                items
        ));
    }

    private static Map<Integer, ConfiguredShopItem> loadItems(ConfigurationSection section, int shopSize) {
        Map<Integer, ConfiguredShopItem> items = new HashMap<>();
        if (section == null) {
            return items;
        }

        for (String itemId : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(itemId);
            if (itemSection == null) {
                continue;
            }

            int slot = itemSection.getInt("slot", -1);
            if (slot < 0 || slot >= shopSize) {
                continue;
            }

            ConfiguredShopItem shopItem = new ConfiguredShopItem(
                    itemId,
                    slot,
                    ShopAction.fromString(itemSection.getString("action")),
                    ShopCurrency.fromString(itemSection.getString("currency")),
                    new BigDecimal(itemSection.getString("price", "0")),
                    createItemSpec(itemSection.getConfigurationSection("item"))
            );
            items.put(slot, shopItem);
        }
        return items;
    }

    private static ShopItemSpec createItemSpec(ConfigurationSection section) {
        if (section == null) {
            return new ShopItemSpec("STONE", 1, "", null, java.util.List.of());
        }

        return new ShopItemSpec(
                section.getString("material", "STONE"),
                Math.max(1, section.getInt("amount", 1)),
                section.getString("name", ""),
                section.getString("model_name", section.getString("model_data", null)),
                section.getStringList("lore")
        );
    }

    private static int normalizeSize(int size) {
        int normalized = Math.max(9, Math.min(54, size));
        return ((normalized + 8) / 9) * 9;
    }
}
