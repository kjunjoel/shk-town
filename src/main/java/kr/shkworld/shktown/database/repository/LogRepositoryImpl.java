package kr.shkworld.shktown.database.repository;

import kr.shkworld.shktown.core.logging.model.LogType;
import kr.shkworld.shktown.core.logging.repository.LogRepository;
import kr.shkworld.shktown.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class LogRepositoryImpl implements LogRepository {
    @Override
    public CompletableFuture<Void> saveSystemLog(LogType type, String message, String dataJson) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO system_logs (log_type, message, data_json) VALUES (?, ?, ?)";

            try (Connection connection = DatabaseManager.getInstance().getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, type.name());
                statement.setString(2, message);
                statement.setString(3, dataJson);
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException("시스템 로그 저장 중 DB 에러 발생: " + type, e);
            }
        });
    }
}
