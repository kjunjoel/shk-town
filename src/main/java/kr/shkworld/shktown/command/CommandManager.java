package kr.shkworld.shktown.command;

import kr.shkworld.shktown.SHKTown;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

public class CommandManager {
    private final SHKTown plugin;

    public CommandManager(SHKTown plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        SHKTownCommand shkTownCommand = new SHKTownCommand(plugin);
        register("shktown", shkTownCommand, shkTownCommand);
    }

    private void register(String label, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = plugin.getCommand(label);
        if (command != null) {
            command.setExecutor(executor);
            if (tabCompleter != null) {
                command.setTabCompleter(tabCompleter);
            }
        } else {
            plugin.getLogger().warning("plugin.yml에 등록되지 않은 명령어입니다: " + label);
        }
    }
}
