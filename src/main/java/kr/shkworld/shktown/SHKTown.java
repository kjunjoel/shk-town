package kr.shkworld.shktown;

import kr.shkworld.shktown.command.CommandManager;
import kr.shkworld.shktown.config.ConfigManager;
import kr.shkworld.shktown.core.economy.repository.AccountRepository;
import kr.shkworld.shktown.core.logging.repository.LogRepository;
import kr.shkworld.shktown.core.economy.repository.TransactionRepository;
import kr.shkworld.shktown.core.economy.repository.UserRepository;
import kr.shkworld.shktown.core.economy.service.AccountService;
import kr.shkworld.shktown.core.economy.service.EconomyService;
import kr.shkworld.shktown.core.logging.service.LogService;
import kr.shkworld.shktown.core.economy.service.UserService;
import kr.shkworld.shktown.core.economy.service.impl.AccountServiceImpl;
import kr.shkworld.shktown.core.economy.service.impl.EconomyServiceImpl;
import kr.shkworld.shktown.core.logging.service.impl.LogServiceImpl;
import kr.shkworld.shktown.core.economy.service.impl.UserServiceImpl;
import kr.shkworld.shktown.core.navigation.service.NavigationService;
import kr.shkworld.shktown.core.shop.service.ShopService;
import kr.shkworld.shktown.core.shop.service.impl.ShopServiceImpl;
import kr.shkworld.shktown.database.DatabaseManager;
import kr.shkworld.shktown.database.repository.AccountRepositoryImpl;
import kr.shkworld.shktown.database.repository.LogRepositoryImpl;
import kr.shkworld.shktown.database.repository.TransactionRepositoryImpl;
import kr.shkworld.shktown.database.repository.UserRepositoryImpl;
import kr.shkworld.shktown.integration.luckperms.LuckPermsHook;
import kr.shkworld.shktown.integration.worldguard.WorldGuardHook;
import kr.shkworld.shktown.listener.EventManager;
import kr.shkworld.shktown.ui.apps.SmartphoneManager;
import kr.shkworld.shktown.ui.apps.navigation.NavigationManager;
import kr.shkworld.shktown.ui.shop.ShopManager;
import kr.shkworld.shktown.ui.apps.taxi.TaxiMapManager;
import kr.shkworld.shktown.util.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SHKTown extends JavaPlugin {
    private ConfigManager configManager;

    private UserService userService;
    private AccountService accountService;
    private EconomyService economyService;
    private LogService logService;
    private ShopService shopService;

    private SmartphoneManager smartphoneManager;
    private TaxiMapManager taxiMapManager;
    private NavigationService navigationService;
    private NavigationManager navigationManager;
    private ShopManager shopManager;

    private final PluginLogger pluginLogger = new PluginLogger() {
        @Override
        public void info(String message) { getLogger().info(message); }
        @Override
        public void warning(String message) { getLogger().warning(message); }
        @Override
        public void severe(String message) { getLogger().severe(message); }
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

        LuckPermsHook luckPermsHook = new LuckPermsHook();
        WorldGuardHook worldGuardHook = new WorldGuardHook();
        if (!luckPermsHook.hook()) {
            getLogger().warning("LuckPerms 플러그인을 찾을 수 없습니다! 일부 기능이 비활성화됩니다.");
        }
        if (!worldGuardHook.hook()) {
            getLogger().warning("WorldGuard 플러그인을 찾을 수 없습니다! 일부 기능이 비활성화됩니다.");
        }

        UserRepository userRepository = new UserRepositoryImpl();
        AccountRepository accountRepository = new AccountRepositoryImpl();
        LogRepository logRepository = new LogRepositoryImpl();
        TransactionRepository transactionRepository = new TransactionRepositoryImpl();

        File dataFolder = getDataFolder();

        this.logService = new LogServiceImpl(dataFolder, logRepository, pluginLogger);
        this.userService = new UserServiceImpl(userRepository, logService);
        this.accountService = new AccountServiceImpl(accountRepository, transactionRepository, logService);
        this.economyService = new EconomyServiceImpl(accountService, transactionRepository);
        this.shopService = new ShopServiceImpl(accountService, userService);

        this.navigationService = new NavigationService();

        this.smartphoneManager = new SmartphoneManager(this);
        this.taxiMapManager = new TaxiMapManager(this);
        this.navigationManager = new NavigationManager(this);
        this.shopManager = new ShopManager(this, shopService);

        this.configManager = new ConfigManager(this);
        reload();

        new EventManager(this).registerEvents();
        new CommandManager(this).registerCommands();

        getLogger().info("SHK TOWN 플러그인이 성공적으로 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        if (accountService != null) {
            accountService.flushAllSync();
        }
        if (userService != null) {
            userService.flushAllSync();
        }
        DatabaseManager.getInstance().close();
        getLogger().info("SHK TOWN 플러그인이 종료되었습니다.");
    }

    public void reload() {
        configManager.loadConfigs();
    }

    public UserService getUserService() { return userService; }
    public AccountService getAccountService() { return accountService; }
    public EconomyService getEconomyService() { return economyService; }
    public LogService getLogService() { return logService; }
    public ShopService getShopService() { return shopService; }
    public NavigationService getNavigationService() {
        return navigationService;
    }
    public SmartphoneManager getSmartphoneManager() {
        return smartphoneManager;
    }
    public TaxiMapManager getTaxiMapManager() {
        return taxiMapManager;
    }
    public NavigationManager getNavigationManager() {
        return navigationManager;
    }
    public ShopManager getShopManager() {
        return shopManager;
    }
}
