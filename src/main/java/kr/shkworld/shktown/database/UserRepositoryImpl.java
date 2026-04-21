package kr.shkworld.shktown.database;

import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.repository.UserRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
                plugin.getLogger().severe("DB에서 UUID" + uuid.toString() +" 의 데이터를 불러오던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return Optional.empty();
        });
    }
    /*

    @Override
    public CompletableFuture<List<RankEntry>> getTopRanksAsync(AccountType type, int page) {
        return CompletableFuture.supplyAsync(() -> {
           int limit = 10;
           int offset = (page - 1) * limit;
           List<RankEntry> rankList = new ArrayList<>();

            String sql = (type == null)
                    ? "SELECT u.name, SUM(a.balance) as total " +
                      "FROM accounts a "+
                      "JOIN users u ON a.owner_uuid = u.uuid " +
                      "GROUP BY a.owner_uuid " +
                      "ORDER BY total DESC LIMIT ? OFFSET ?"
                    : "SELECT u.name, SUM(a.balance) as total " +
                      "FROM accounts a "+
                      "JOIN users u ON a.owner_uuid = u.uuid " +
                      "WHERE a.account_type = ? " +
                      "GROUP BY a.owner_uuid " +
                      "ORDER BY total DESC LIMIT ? OFFSET ?";

           try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

               if (type == null) {
                   pstmt.setInt(1, limit);
                   pstmt.setInt(2, offset);
               } else {
                   pstmt.setInt(1, type.getCode());
                   pstmt.setInt(2, limit);
                   pstmt.setInt(3, offset);
               }

               try (ResultSet rs = pstmt.executeQuery()) {
                   while (rs.next()) {
                       String playerName = rs.getString("name");
                       BigDecimal amount = rs.getBigDecimal("total");

                       if (playerName == null) playerName = "알 수 없는 유저";

                       rankList.add(new RankEntry(playerName, amount));
                   }
               }
           } catch (SQLException e) {
               plugin.getLogger().severe("자산 순위 DB에서 로드 중 오류 발생: " + e.getMessage());
           }
           return rankList;
        });
    }
    */
}
