package kr.shkworld.shktown.command;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.core.repository.UserRepository;
import kr.shkworld.shktown.core.service.EconomyService;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.service.UserService;
import org.bukkit.command.CommandExecutor;

public class CommandManager {
    private final SHKTown plugin;
    private final EconomyService economyService;
    private final LogService logService;
    private final UserService userService;

    public CommandManager(SHKTown plugin, EconomyService economyService, LogService logService, UserService userService) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.logService = logService;
        this.userService = userService;
    }

    public void registerCommands() {
        MoneyCommand moneyCommand = new MoneyCommand(economyService, logService, userService);
        register("돈", moneyCommand);
        register("마을", new TownCommand());
        register("재산", moneyCommand);
        register("캐시", moneyCommand);
    }

    private void register(String label, CommandExecutor executor) {
        var command = plugin.getCommand(label);
        if (command != null) {
            command.setExecutor(executor);
        }
    }
}
