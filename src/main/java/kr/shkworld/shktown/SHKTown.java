package kr.shkworld.shktown;

import kr.shkworld.shktown.command.CommandManager;
import kr.shkworld.shktown.core.repository.AccountRepository;
import kr.shkworld.shktown.core.repository.EconomyRepository;
import kr.shkworld.shktown.core.repository.LogRepository;
import kr.shkworld.shktown.core.repository.UserRepository;
import kr.shkworld.shktown.core.service.AccountService;
import kr.shkworld.shktown.core.service.EconomyService;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.service.UserService;
import kr.shkworld.shktown.core.service.impl.AccountServiceImpl;
import kr.shkworld.shktown.core.service.impl.EconomyServiceImpl;
import kr.shkworld.shktown.core.service.impl.LogServiceImpl;
import kr.shkworld.shktown.core.service.impl.UserServiceImpl;
import kr.shkworld.shktown.core.service.impl.wealth.LiquidWealth;
import kr.shkworld.shktown.core.util.PluginLogger;
import kr.shkworld.shktown.database.AccountRepositoryImpl;
import kr.shkworld.shktown.database.DatabaseManager;
import kr.shkworld.shktown.database.EconomyRepositoryImpl;
import kr.shkworld.shktown.database.LogRepositoryImpl;
import kr.shkworld.shktown.database.UserRepositoryImpl;
import kr.shkworld.shktown.integration.VaultEconomyProvider;
import kr.shkworld.shktown.listener.PlayerJoinListener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class SHKTown extends JavaPlugin {
    private UserService userService;
    private AccountService accountService;
    private EconomyService economyService;
    private LogService logService;

    private final PluginLogger pluginLogger = new PluginLogger() {
        @Override
        public void info(String message) {
            getLogger().info(message);
        }

        @Override
        public void warning(String message) {
            getLogger().warning(message);
        }

        @Override
        public void severe(String message) {
            getLogger().severe(message);
        }
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            DatabaseManager.getInstance().setup(this);
        } catch (Exception e) {
            getLogger().severe("DB 연결에 실패했습니다.\n" + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        UserRepository userRepository = new UserRepositoryImpl(this);
        AccountRepository accountRepository = new AccountRepositoryImpl(this);
        EconomyRepository economyRepository = new EconomyRepositoryImpl(this);
        LogRepository logRepository = new LogRepositoryImpl(this);

        this.logService = new LogServiceImpl(logRepository, pluginLogger);
        this.userService = new UserServiceImpl(userRepository, accountRepository, pluginLogger);
        this.accountService = new AccountServiceImpl(userService, accountRepository);
        this.economyService = new EconomyServiceImpl(economyRepository, userService, accountService, logService, pluginLogger);
        this.economyService.addWealthComponents(new LiquidWealth(userService));

        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(userService, accountService, logService),
                this
        );

        new CommandManager(this).registerCommands();

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            pluginLogger.severe("Vault 플러그인을 찾을 수 없습니다! 경제 연동이 비활성화됩니다.");
            return;
        }

        getServer().getServicesManager().register(
                net.milkbowl.vault.economy.Economy.class,
                new VaultEconomyProvider(accountService, economyService),
                this,
                ServicePriority.Highest
        );

        getLogger().info("SHK TOWN 플러그인이 성공적으로 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        userService.saveAllSync();

        DatabaseManager.getInstance().close();

        getLogger().info("SHK TOWN 플러그인이 종료되었습니다.");
    }

    public UserService getUserService() { return userService; }
    public AccountService getAccountService() { return accountService; }
    public EconomyService getEconomyService() { return economyService; }
    public LogService getLogService() { return logService; }
}
