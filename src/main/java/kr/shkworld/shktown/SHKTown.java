package kr.shkworld.shktown;

import kr.shkworld.shktown.command.CommandManager;
import kr.shkworld.shktown.core.repository.LogRepository;
import kr.shkworld.shktown.core.repository.UserRepository;
import kr.shkworld.shktown.core.service.EconomyService;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.service.UserService;
import kr.shkworld.shktown.core.service.impl.EconomyServiceImpl;
import kr.shkworld.shktown.core.service.impl.LogServiceImpl;
import kr.shkworld.shktown.core.service.impl.UserServiceImpl;
import kr.shkworld.shktown.database.DatabaseManager;
import kr.shkworld.shktown.database.LogRepositoryImpl;
import kr.shkworld.shktown.database.UserRepositoryImpl;
import kr.shkworld.shktown.listener.PlayerJoinListener;
import org.bukkit.plugin.java.JavaPlugin;

public class SHKTown extends JavaPlugin {
    private LogRepository logRepository;
    private UserRepository userRepository;
    private EconomyService economyService;
    private LogService logService;
    private UserService userService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            DatabaseManager.getInstance().setup(this);
        } catch (Exception e) {
            getLogger().severe("DB 연결 실패: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.logRepository = new LogRepositoryImpl(this);
        this.userRepository = new UserRepositoryImpl(this);
        this.logService = new LogServiceImpl(this.getLogger(), logRepository);
        this.userService = new UserServiceImpl(userRepository);
        this.economyService = new EconomyServiceImpl(logService, userService, userRepository);

        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(userService, logService),
                this
        );

        new CommandManager(this, economyService, logService, userService).registerCommands();

        getLogger().info("SHK TOWN 플러그인이 성공적으로 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        DatabaseManager.getInstance().close();

        getLogger().info("SHK TOWN 플러그인이 종료되었습니다.");
    }
}
