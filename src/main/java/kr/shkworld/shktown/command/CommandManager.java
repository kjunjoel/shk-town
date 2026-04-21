package kr.shkworld.shktown.command;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.core.service.EconomyService;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.service.UserService;
import org.bukkit.command.CommandExecutor;

public class CommandManager {
    private final SHKTown plugin;

    public CommandManager(SHKTown plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        MoneyCommand moneyCommand = new MoneyCommand(this.plugin);
        register("돈", moneyCommand);
        register("마을", new TownCommand());
        register("캐시", moneyCommand);
    }

    private void register(String label, CommandExecutor executor) {
        var command = plugin.getCommand(label);
        if (command != null) {
            command.setExecutor(executor);
        }
    }
}
