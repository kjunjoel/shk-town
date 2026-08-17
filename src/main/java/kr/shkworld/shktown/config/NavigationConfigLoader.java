package kr.shkworld.shktown.config;

import kr.shkworld.shktown.core.navigation.model.Position;
import kr.shkworld.shktown.core.navigation.service.NavigationService;
import kr.shkworld.shktown.ui.apps.navigation.NavigationManager;
import kr.shkworld.shktown.util.GUIUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class NavigationConfigLoader {
    private NavigationConfigLoader() {
    }

    public static void loadNavigationConfig(FileConfiguration config, NavigationService service, NavigationManager manager) {
        if (config == null || service == null || manager == null) return;
        ConfigurationSection settings = config.getConfigurationSection("settings");
        service.setArrivalRadius(settings != null ? settings.getDouble("arrival_radius", 0.0) : 0.0);
        ConfigurationSection messages = config.getConfigurationSection("messages");
        manager.configure(
                settings != null ? settings.getString("title", "") : "",
                settings != null ? settings.getString("betterhud_pointer_id", "") : "",
                settings != null ? settings.getInt("update_interval_ticks", 1) : 1,
                messages != null && messages.getBoolean("use_global_prefix", false),
                messages != null ? messages.getString("started", "") : "",
                messages != null ? messages.getString("stopped", "") : "",
                messages != null ? messages.getString("arrived", "") : "",
                messages != null ? messages.getString("different_world", "") : ""
        );
        service.clearDestinations();
        manager.clearDestinationItems();
        ConfigurationSection destinations = config.getConfigurationSection("destinations");
        if (destinations == null) return;
        for (String key : destinations.getKeys(false)) {
            ConfigurationSection destination = destinations.getConfigurationSection(key);
            if (destination == null) continue;
            ConfigurationSection location = destination.getConfigurationSection("location");
            if (location == null) continue;
            Position position = new Position(location.getString("world"), location.getDouble("x"), location.getDouble("y"), location.getDouble("z"));
            service.registerDestination(key, position);
            Material material = Material.matchMaterial(destination.getString("material", "COMPASS"));
            manager.registerDestinationItem(key, destination.getString("name", key), GUIUtil.createItem(
                    material != null ? material : Material.COMPASS,
                    destination.getString("name", key),
                    destination.getString("model_data", destination.getString("model_name", "0"))
            ));
        }
    }
}
