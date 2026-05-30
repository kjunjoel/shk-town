package kr.shkworld.shktown.database.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.bukkit.plugin.java.JavaPlugin;

import kr.shkworld.shktown.core.model.Town;
import kr.shkworld.shktown.core.repository.TownRepository;
import kr.shkworld.shktown.database.DatabaseManager;

public class TownRepositoryImpl implements TownRepository {
    private final JavaPlugin plugin;

    public TownRepositoryImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Optional<Town>> findByID(long id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * " +
                         "FROM towns " +
                         "WHERE id = ?";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setLong(1, id);
            
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        return Optional.of(new Town(
                                id,
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("name"),
                                UUID.fromString(rs.getString("mayor_uuid")),
                                rs.getLong("nation_id")
                        ));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 마을 ID " + id + "의 데이터를 불러오던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<Town>> findByUUID(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * " +
                         "FROM towns " +
                         "WHERE uuid = ?";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, uuid);
            
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        return Optional.of(new Town(
                                rs.getLong("id"),
                                UUID.fromString(uuid),
                                rs.getString("name"),
                                UUID.fromString(rs.getString("mayor_uuid")),
                                rs.getLong("nation_id")
                        ));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 마을 UUID " + uuid + "의 데이터를 불러오던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<Town>> findByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * " +
                         "FROM towns " +
                         "WHERE name = ?";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, name);
            
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        return Optional.of(new Town(
                                rs.getLong("id"),
                                UUID.fromString(rs.getString("uuid")),
                                name,
                                UUID.fromString(rs.getString("mayor_uuid")),
                                rs.getLong("nation_id")
                        ));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 마을 이름 " + name + "의 데이터를 불러오던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Town> saveTown(Town town) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO towns (id, uuid, name, mayor_uuid, nation_id) " +
                         "VALUES (?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE " +
                         "name = ?, mayor_uuid = ?, nation_id = ?";
            
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (town.getID() <= 0) preparedStatement.setNull(1, Types.BIGINT);
                else preparedStatement.setLong(1, town.getID());

                preparedStatement.setString(2, town.getUUID().toString());
                preparedStatement.setString(3, town.getName());
                preparedStatement.setString(4, town.getMayorUUID().toString());
                preparedStatement.setLong(5, town.getNationID());
                preparedStatement.setString(6, town.getName());
                preparedStatement.setString(7, town.getMayorUUID().toString());
                preparedStatement.setLong(8, town.getNationID());

                preparedStatement.executeUpdate();

                try (ResultSet rs = preparedStatement.getGeneratedKeys()) {
                    if (rs.next()) {
                        long generatedID = rs.getLong(1);
                        plugin.getLogger().info(town.getName() + " 마을이 ID " + generatedID + "번으로 저장되었습니다.");
                        return town.withID(generatedID);
                    }
                }
                return town;

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에 마을 ID " + town.getID() + "의 데이터를 저장하던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteTown(long id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM towns " +
                         "WHERE id = ?";
            
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setLong(1, id);
                preparedStatement.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 마을 ID " + id + "의 데이터를 삭제하던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);
            }
        });
    }
    
}