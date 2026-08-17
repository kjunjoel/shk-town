package kr.shkworld.shktown.config;

import kr.shkworld.shktown.ui.apps.SmartphoneManager;
import kr.shkworld.shktown.util.GUIUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SmartphoneConfigLoader {
    private SmartphoneConfigLoader() {}

    public static void loadSmartphoneConfig(FileConfiguration config, SmartphoneManager smartphoneManager) {
        if (config == null || smartphoneManager == null) return;

        String title = config.getString("title", "");
        smartphoneManager.setMainTitle(title);

        ConfigurationSection homeSection = config.getConfigurationSection("home_button");
        if (homeSection != null) {
            smartphoneManager.setHomeButton(createItem(homeSection));
        }

        ConfigurationSection navigationButtons = config.getConfigurationSection("navigation_buttons");
        if (navigationButtons != null) {
            ConfigurationSection previous = navigationButtons.getConfigurationSection("previous_button");
            if (previous != null) smartphoneManager.setNavigationPreviousButton(createItem(previous));
            ConfigurationSection next = navigationButtons.getConfigurationSection("next_button");
            if (next != null) smartphoneManager.setNavigationNextButton(createItem(next));
        }

        Map<Integer, ItemStack> appItems = new HashMap<>();
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection == null) continue;

                int slot = itemSection.getInt("slot", 0);
                ItemStack itemStack = createItem(itemSection);
                appItems.put(slot, itemStack);
            }
        }
        smartphoneManager.setAppItems(appItems);
    }

    private static ItemStack createItem(ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("material", "PAPER"));
        return GUIUtil.createItem(
                material != null ? material : Material.PAPER,
                section.getString("name", ""),
                section.getString("model_name", section.getString("model_data", null)),
                section.getStringList("lore").toArray(new String[0])
        );
    }
}
