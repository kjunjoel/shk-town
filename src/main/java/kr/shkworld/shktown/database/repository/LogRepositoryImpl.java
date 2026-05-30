package kr.shkworld.shktown.database.repository;

import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.EconomyLog;
import kr.shkworld.shktown.core.model.LogType;
import kr.shkworld.shktown.core.repository.LogRepository;
import kr.shkworld.shktown.database.DatabaseManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LogRepositoryImpl implements LogRepository {
    private final JavaPlugin plugin;
    private final Executor logExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Log-Worker-Thread");
        thread.setDaemon(true);
        return thread;
    });

    public LogRepositoryImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> saveEconomyLog(AccountType type, String targetID, String accountNumber, BigDecimal amount, BigDecimal balanceAfter, Enum<?> reason, String detail) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO economy_logs (asset_type, target_id, account_number, amount, balance_after, reason, detail) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setInt(1, type.getCode());
                preparedStatement.setString(2, targetID);
                preparedStatement.setString(3, accountNumber);
                preparedStatement.setBigDecimal(4, amount);
                preparedStatement.setBigDecimal(5, balanceAfter);
                preparedStatement.setString(6, reason.name());
                preparedStatement.setString(7, detail);
                preparedStatement.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에 " + targetID +"님의 경제 관련 로그를 저장하던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
        }, logExecutor);
    }

    @Override
    public CompletableFuture<Void> saveSystemLog(LogType type, String message, String dataJson) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO system_logs (log_type, message, data_json) " +
                         "VALUES (?, ?, ?)";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, type.name());
                preparedStatement.setString(2, message);
                preparedStatement.setString(3, dataJson);
                preparedStatement.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에 " + type.name() + "관련 시스템 관련 로그를 저장하던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
        }, logExecutor);
    }

    @Override
    public CompletableFuture<List<EconomyLog>> findRecentEconomyLogs(String targetID, AccountType type, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<EconomyLog> economyLogs = new ArrayList<>();
            String sql = "SELECT * " +
                         "FROM economy_logs " +
                         "WHERE asset_type = ? AND target_id = ? " +
                         "ORDER BY created_at DESC " +
                         "LIMIT ?";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setInt(1, type.getCode());
                preparedStatement.setString(2, targetID);
                preparedStatement.setInt(3, limit);

                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        economyLogs.add(new EconomyLog(
                                rs.getLong("id"),
                                type,
                                targetID,
                                rs.getString("account_number"),
                                rs.getBigDecimal("amount"),
                                rs.getBigDecimal("balance_after"),
                                rs.getString("reason"),
                                rs.getString("detail"),
                                rs.getTimestamp("created_at").toLocalDateTime()
                        ));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 " + targetID + "의 경제 로그를 검색하던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return economyLogs;
        });
    }
}
