package kr.shkworld.shktown.config;

import kr.shkworld.shktown.core.navigation.model.Position;
import kr.shkworld.shktown.ui.apps.taxi.TaxiMapManager;
import kr.shkworld.shktown.util.GUIUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class TaxiConfigLoader {
    private TaxiConfigLoader() {
    }

    public static void loadTaxiConfig(JavaPlugin plugin, FileConfiguration config, TaxiMapManager mapManager) {
        if (config == null || mapManager == null) return;

        Map<String, Position> positions = new HashMap<>();
        Map<String, ItemStack> stopItems = new HashMap<>();
        Map<String, String> stopNames = new HashMap<>();
        loadStops(config, positions, stopItems, stopNames);
        loadUi(config, mapManager);
        loadAccess(config, mapManager);
        loadMaps(plugin, config, mapManager, positions, stopItems, stopNames);
    }

    private static void loadStops(FileConfiguration config, Map<String, Position> positions,
                                  Map<String, ItemStack> items, Map<String, String> names) {
        ConfigurationSection stops = config.getConfigurationSection("stops");
        if (stops == null) return;
        for (String key : stops.getKeys(false)) {
            ConfigurationSection stop = stops.getConfigurationSection(key);
            if (stop == null) continue;
            ConfigurationSection location = stop.getConfigurationSection("location");
            if (location == null) continue;
            Position position = new Position(location.getString("world"), location.getDouble("x"), location.getDouble("y"),
                    location.getDouble("z"), (float) location.getDouble("yaw", 0), (float) location.getDouble("pitch", 0));
            String name = stop.getString("name", key);
            positions.put(key, position);
            names.put(key, name);
            items.put(key, createItem(stop, "&e🚕 " + name,
                    "&fX: " + position.x() + ", Y: " + position.y() + ", Z: " + position.z(),
                    "&7클릭시 해당 승강장으로 이동합니다."));
        }
    }

    private static void loadUi(FileConfiguration config, TaxiMapManager manager) {
        ConfigurationSection settings = config.getConfigurationSection("settings");
        ConfigurationSection main = config.getConfigurationSection("main_screen");
        Map<Integer, ItemStack> buttons = new HashMap<>();
        Map<Integer, String> maps = new HashMap<>();
        ConfigurationSection buttonSection = main != null ? main.getConfigurationSection("buttons") : null;
        if (buttonSection != null) {
            for (String key : buttonSection.getKeys(false)) {
                ConfigurationSection button = buttonSection.getConfigurationSection(key);
                if (button == null) continue;
                int slot = button.getInt("slot", -1);
                if (slot < 0 || slot >= 54) continue;
                buttons.put(slot, createItem(button, button.getString("name", key),
                        "OPEN_MAP".equalsIgnoreCase(button.getString("action")) ? "&7목적지를 선택하여 택시를 호출합니다." : "&7준비 중인 서비스입니다."));
                if ("OPEN_MAP".equalsIgnoreCase(button.getString("action"))) maps.put(slot, button.getString("map", ""));
            }
        }
        ConfigurationSection ui = config.getConfigurationSection("ui");
        ConfigurationSection messages = config.getConfigurationSection("messages");
        manager.configureUi(
                settings != null ? settings.getString("default_map", "") : "",
                main != null ? main.getString("title", "") : "", buttons, maps,
                messages != null ? messages.getString("unavailable_service", "") : "",
                messages != null && messages.getBoolean("use_global_prefix", false),
                messages != null ? messages.getString("arrived", "") : "",
                messages != null ? messages.getString("loading_app", "") : "",
                ui != null ? ui.getString("title_main", "") : "", ui != null ? ui.getString("title_sub", "") : "",
                ui != null ? ui.getInt("title_fade_in_ms", 0) : 0, ui != null ? ui.getInt("title_stay_ms", 0) : 0,
                ui != null ? ui.getInt("title_fade_out_ms", 0) : 0, ui != null ? ui.getLong("teleport_delay_ticks", 0) : 0
        );
    }

    private static void loadAccess(FileConfiguration config, TaxiMapManager manager) {
        ConfigurationSection access = config.getConfigurationSection("access");
        ConfigurationSection callPass = access != null ? access.getConfigurationSection("call_pass") : null;
        ConfigurationSection npc = access != null ? access.getConfigurationSection("npc") : null;
        ConfigurationSection messages = config.getConfigurationSection("messages");
        manager.configureAccess(createItem(callPass, "&e택시 호출권"),
                messages != null ? messages.getString("no_call_pass", "") : "",
                npc != null && npc.getBoolean("enabled"), npc != null ? npc.getString("name", "") : "");
    }

    private static void loadMaps(JavaPlugin plugin, FileConfiguration config, TaxiMapManager manager, Map<String, Position> positions,
                                 Map<String, ItemStack> items, Map<String, String> names) {
        manager.clearMaps();
        ConfigurationSection maps = config.getConfigurationSection("maps");
        if (maps == null) return;
        for (String id : maps.getKeys(false)) {
            ConfigurationSection map = maps.getConfigurationSection(id);
            if (map == null) continue;
            Map<String, String> navigation = new HashMap<>();
            ConfigurationSection nav = map.getConfigurationSection("navigation");
            if (nav != null) for (String direction : nav.getKeys(false)) navigation.put(direction, nav.getString(direction));
            Map<Integer, ItemStack> mapItems = new HashMap<>();
            Map<Integer, Position> mapPositions = new HashMap<>();
            Map<Integer, String> mapNames = new HashMap<>();
            ConfigurationSection placements = map.getConfigurationSection("placements");
            if (placements != null) for (String stopKey : placements.getKeys(false)) {
                int slot = placements.getInt(stopKey, -1);
                if (!positions.containsKey(stopKey) || slot < 0 || slot >= 54) {
                    plugin.getLogger().warning("Invalid taxi placement: " + stopKey);
                    continue;
                }
                mapItems.put(slot, items.get(stopKey));
                mapPositions.put(slot, positions.get(stopKey));
                mapNames.put(slot, names.get(stopKey));
            }
            manager.registerMap(id, map.getString("title", ""), navigation, mapItems, mapPositions, mapNames);
        }
    }

    private static ItemStack createItem(ConfigurationSection section, String defaultName, String... lore) {
        String materialName = section != null ? section.getString("material", "PAPER") : "PAPER";
        Material material = Material.matchMaterial(materialName);
        return GUIUtil.createItem(material != null ? material : Material.PAPER,
                section != null ? section.getString("name", defaultName) : defaultName,
                section != null ? section.getString("model_data", section.getString("model_name", "0")) : "0", lore);
    }
}
