package kr.shkworld.shktown.config;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        Map<String, FileConfiguration> shopConfigs = loadConfigDirectory(
                "configs/shops",
                "configs/shops/general.yml"
        );

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

        ShopConfigLoader.loadShopConfigs(
                shopConfigs,
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

    private Map<String, FileConfiguration> loadConfigDirectory(String directoryName, String... defaultResources) {
        File directory = new File(plugin.getDataFolder(), directoryName);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        for (String defaultResource : defaultResources) {
            File defaultFile = new File(plugin.getDataFolder(), defaultResource);
            if (!defaultFile.exists()) {
                plugin.saveResource(defaultResource, false);
            }
        }

        Map<String, FileConfiguration> loadedConfigs = new LinkedHashMap<>();
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return loadedConfigs;
        }

        Arrays.sort(files, (left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            loadedConfigs.put(file.getName(), config);
        }
        return loadedConfigs;
    }

    public FileConfiguration getConfig(String key) {
        return configs.get(key);
    }
}
