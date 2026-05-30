package kr.shkworld.shktown.database.repository;

import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.repository.UserRepository;
import kr.shkworld.shktown.database.DatabaseManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class UserRepositoryImpl implements UserRepository {
    private final JavaPlugin plugin;

    public UserRepositoryImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> saveUser(User user) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveUserSync(user);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public void saveUserSync(User user) {
        String sql = "INSERT INTO users (uuid, name, last_login) " +
                     "VALUES (?, ?, NOW()) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "name = ?, cash = ?, town_id = ?, nation_id = ?, last_login = NOW()";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, user.getUUID().toString());
            preparedStatement.setString(2, user.getName());
            preparedStatement.setString(3, user.getName());
            preparedStatement.setBigDecimal(4, user.getCash());
            preparedStatement.setLong(5, user.getTownID());
            preparedStatement.setLong(6, user.getNationID());
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("DB에 " + user.getName() +" 님의 데이터를 저장하던 중 오류가 발생하였습니다.\n" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Optional<User>> loadUser(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT name, cash, town_id, nation_id " +
                         "FROM users " +
                         "WHERE uuid = ?";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, uuid.toString());

                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("name");
                        BigDecimal cash = rs.getBigDecimal("cash");
                        long townID = rs.getLong("town_id");
                        long nationID = rs.getLong("nation_id");

                        User user = new User(uuid, name, cash, new ArrayList<>());
                        user.setTownID(townID);
                        user.setNationID(nationID);

                        return Optional.of(user);
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 UUID " + uuid.toString() + "의 데이터를 불러오던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return Optional.empty();
        });
    }
    
    @Override
    public CompletableFuture<List<UUID>> findUUIDsByTownID(long townID) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid " +
                         "FROM users " +
                         "WHERE town_id = ?";
            List<UUID> list = new ArrayList<>();

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setLong(1, townID);

                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        list.add(UUID.fromString(rs.getString("uuid")));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 마을 ID " + townID + "의 데이터를 불러오던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return list;
        });
    }

    @Override
    public CompletableFuture<List<UUID>> findUUIDsByNationID(long nationID) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid " +
                         "FROM users " +
                         "WHERE nation_id = ?";
            List<UUID> list = new ArrayList<>();

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setLong(1, nationID);

                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        list.add(UUID.fromString(rs.getString("uuid")));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 국가 ID " + nationID + "의 데이터를 불러오던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return list;
        });
    }
}
