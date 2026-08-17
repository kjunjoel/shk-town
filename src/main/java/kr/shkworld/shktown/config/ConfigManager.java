package kr.shkworld.shktown.config;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final SHKTown plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public ConfigManager(SHKTown plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        configs.clear();

        loadConfig("global", "config.yml");

        loadConfig("smartphone", "configs/smartphone.yml");
        loadConfig("taxi", "configs/taxi.yml");
        loadConfig("navigation", "configs/navigation.yml");
        loadConfig("shops", "configs/shops.yml");

        loadGlobalConfig(getConfig("global"));

        SmartphoneConfigLoader.loadSmartphoneConfig(
                getConfig("smartphone"),
                plugin.getSmartphoneManager()
        );

        TaxiConfigLoader.loadTaxiConfig(
                plugin,
                getConfig("taxi"),
                plugin.getTaxiMapManager()
        );

        NavigationConfigLoader.loadNavigationConfig(
                getConfig("navigation"),
                plugin.getNavigationService(),
                plugin.getNavigationManager()
        );

        ShopConfigLoader.loadShopConfig(
                getConfig("shops"),
                plugin.getShopManager()
        );
    }

    private static void loadGlobalConfig(FileConfiguration config) {
        if (config == null) return;
        MessageUtil.initGlobalConfig(
                config.getString("prefix", ""),
                config.getString("reload_success", ""),
                config.getString("no_permission", "")
        );
    }

    private void loadConfig(String key, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
        }

        configs.put(key, config);
    }

    public FileConfiguration getConfig(String key) {
        return configs.get(key);
    }
}
