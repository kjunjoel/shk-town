package kr.shkworld.shktown.database.repository;

import kr.shkworld.shktown.core.economy.model.CashReason;
import kr.shkworld.shktown.core.economy.model.User;
import kr.shkworld.shktown.core.economy.repository.UserRepository;
import kr.shkworld.shktown.database.DatabaseManager;
import kr.shkworld.shktown.database.UuidBinaryConverter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class UserRepositoryImpl implements UserRepository {

    @Override
    public void saveUserSync(User user) throws SQLException {
        synchronized (user) {
            String sql = "INSERT INTO users (uuid, name, cash, last_login) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE name = ?, last_login = ?";

            try (Connection connection = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setBytes(1, UuidBinaryConverter.toBytes(user.getUuid()));
                preparedStatement.setString(2, user.getName());
                preparedStatement.setBigDecimal(3, user.getCash());
                preparedStatement.setTimestamp(4, toTimestamp(user.getLastLogin()));

                preparedStatement.setString(5, user.getName());
                preparedStatement.setTimestamp(6, toTimestamp(user.getLastLogin()));

                preparedStatement.executeUpdate();
            }
        }
    }

    @Override
    public CompletableFuture<Void> saveUserAsync(User user) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveUserSync(user);
            } catch (SQLException e) {
                throw new CompletionException("User 비동기 저장 중 DB 에러 발생: " + user.getUuid(), e);
            }
        });
    }

    @Override
    public Optional<User> loadUserSync(UUID uuid) throws SQLException {
        try (Connection connection = DatabaseManager.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM users WHERE uuid = ?")) {
            preparedStatement.setBytes(1, UuidBinaryConverter.toBytes(uuid));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    User user = new User(
                            uuid,
                            resultSet.getString("name"),
                            resultSet.getBigDecimal("cash"),
                            toLocalDateTime(resultSet.getTimestamp("last_login")),
                            toLocalDateTime(resultSet.getTimestamp("first_login"))
                    );
                    return Optional.of(user);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Optional<User>> loadUserAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadUserSync(uuid);
            } catch (SQLException e) {
                throw new CompletionException("User 비동기 조회 중 DB 에러 발생: " + uuid, e);
            }
        });
    }

    @Override
    public Optional<User> addCashSync(UUID uuid, BigDecimal amount, CashReason reason, String detail) throws SQLException {
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || reason == null) {
            return Optional.empty();
        }
        return adjustCashSync(uuid, amount, reason, detail);
    }

    @Override
    public CompletableFuture<Optional<User>> addCashAsync(UUID uuid, BigDecimal amount, CashReason reason, String detail) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return addCashSync(uuid, amount, reason, detail);
            } catch (SQLException e) {
                throw new CompletionException("Cash 비동기 지급 중 DB 에러 발생: " + uuid, e);
            }
        });
    }

    @Override
    public Optional<User> subtractCashSync(UUID uuid, BigDecimal amount, CashReason reason, String detail) throws SQLException {
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || reason == null) {
            return Optional.empty();
        }
        return adjustCashSync(uuid, amount.negate(), reason, detail);
    }

    @Override
    public CompletableFuture<Optional<User>> subtractCashAsync(UUID uuid, BigDecimal amount, CashReason reason, String detail) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return subtractCashSync(uuid, amount, reason, detail);
            } catch (SQLException e) {
                throw new CompletionException("Cash 비동기 차감 중 DB 에러 발생: " + uuid, e);
            }
        });
    }

    private Optional<User> adjustCashSync(
            UUID uuid,
            BigDecimal signedAmount,
            CashReason reason,
            String detail
    ) throws SQLException {
        try (Connection connection = DatabaseManager.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                Optional<User> userOpt = lockUser(connection, uuid);
                if (userOpt.isEmpty()) {
                    connection.rollback();
                    return Optional.empty();
                }

                User user = userOpt.get();
                BigDecimal balanceAfter = user.getCash().add(signedAmount);
                if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
                    connection.rollback();
                    return Optional.empty();
                }

                updateCash(connection, uuid, balanceAfter);
                insertCashLog(connection, uuid, signedAmount, balanceAfter, reason, detail);
                connection.commit();

                return Optional.of(new User(
                        uuid,
                        user.getName(),
                        balanceAfter,
                        user.getLastLogin(),
                        user.getFirstLogin()
                ));
            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        }
    }

    private Optional<User> lockUser(Connection connection, UUID uuid) throws SQLException {
        String sql = "SELECT name, cash, last_login, first_login FROM users WHERE uuid = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinaryConverter.toBytes(uuid));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new User(
                        uuid,
                        resultSet.getString("name"),
                        resultSet.getBigDecimal("cash"),
                        toLocalDateTime(resultSet.getTimestamp("last_login")),
                        toLocalDateTime(resultSet.getTimestamp("first_login"))
                ));
            }
        }
    }

    private void updateCash(Connection connection, UUID uuid, BigDecimal balanceAfter) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET cash = ? WHERE uuid = ?")) {
            statement.setBigDecimal(1, balanceAfter);
            statement.setBytes(2, UuidBinaryConverter.toBytes(uuid));
            statement.executeUpdate();
        }
    }

    private void insertCashLog(
            Connection connection,
            UUID uuid,
            BigDecimal amount,
            BigDecimal balanceAfter,
            CashReason reason,
            String detail
    ) throws SQLException {
        String sql = "INSERT INTO cash_logs (user_uuid, amount, balance_after, reason, detail) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinaryConverter.toBytes(uuid));
            statement.setBigDecimal(2, amount);
            statement.setBigDecimal(3, balanceAfter);
            statement.setString(4, reason.name());
            statement.setString(5, detail);
            statement.executeUpdate();
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    private Timestamp toTimestamp(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : Timestamp.valueOf(dateTime);
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
