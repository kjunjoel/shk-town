package kr.shkworld.shktown.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void setup(JavaPlugin plugin) throws SQLException {
        String host = plugin.getConfig().getString("database.host");
        int port = plugin.getConfig().getInt("database.port");
        String dbName = plugin.getConfig().getString("database.database");
        String username = plugin.getConfig().getString("database.username");
        String password = plugin.getConfig().getString("database.password");

        setup(host, port, dbName, username, password);
    }

    public synchronized void setup(
            String host, int port, String dbName,
            String username, String password
    ) throws SQLException {
        close();
        HikariConfig config = new HikariConfig();

        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                host, port, dbName);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(5_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);

        this.dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1)) throw new SQLException("유효하지 않은 DB 연결입니다.");
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DatabaseManager가 초기화되지 않았습니다.");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
