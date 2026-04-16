package kr.shkworld.shktown.database;

import kr.shkworld.shktown.core.model.EconomyLog;
import kr.shkworld.shktown.core.repository.LogRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogRepositoryImpl implements LogRepository {
    private final JavaPlugin plugin;

    public LogRepositoryImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void insertLog(String logType, String targetID, String message, String dataJson) {
        String sql = "INSERT INTO system_logs (log_type, target_id, message, detail_data, created_at) " +
                     "VALUES (?, ?, ?, ?, NOW())";

        executeAsync(sql, logType, targetID, message, dataJson);
    }

    @Override
    public void insertLogEconomy(String uuid, String accountNumber, String action, BigDecimal amount, String reason) {
        String sql = "INSERT INTO economy_logs (target_id, account_number, action_type, amount, reason, created_at) "+
                     "VALUES (?, ?, ?, ?, ?, NOW())";

        executeAsync(sql, uuid, accountNumber, action, amount, reason);
    }

    @Override
    public List<EconomyLog> findRecentEconomyLogs(String accountNumber, int limit) {
        String sql = "SELECT * FROM economy_logs WHERE account_number = ? ORDER BY created_at DESC LIMIT ?";
        List<EconomyLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, accountNumber);
            pstmt.setInt(2, limit);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                logs.add(new EconomyLog(
                        rs.getLong("id"),
                        rs.getString("target_id"),
                        rs.getString("account_number"),
                        rs.getString("action_type"),
                        rs.getBigDecimal("amount"),
                        rs.getString("reason"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("로그 조회 중 오류 발생: " + e.getMessage());
        }

        return logs;
    }

    private void executeAsync(String sql, Object... params) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = connection.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }

                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("DB 비동기 저장 중 오류 발생: " + e.getMessage());
            }
        });
    }
}
