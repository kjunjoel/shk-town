package kr.shkworld.shktown.database;

import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.RankEntry;
import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.repository.UserRepository;
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

public class UserRepositoryImpl implements UserRepository {
    private final JavaPlugin plugin;

    public UserRepositoryImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<User> loadUserFromDB(UUID uuid) {
        String sql = "SELECT u.name, u.town_id, u.nation_id, a.account_number, a.balance, a.account_type " +
                     "FROM users u " +
                     "LEFT JOIN accounts a ON u.uuid = a.owner_uuid " +
                     "WHERE u.uuid = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            User user = null;
            while (rs.next()) {
                if (user == null) {
                    String name = rs.getString("name");
                    long townID = rs.getLong("town_id");
                    if (rs.wasNull()) townID = -1L;
                    long nationID = rs.getLong("nation_id");
                    if (rs.wasNull()) nationID = -1L;
                    user = new User(uuid, name, townID, nationID);
                }

                String accNum = rs.getString("account_number");
                if (accNum != null) {
                    AccountType type = AccountType.fromCode(rs.getInt("account_type"));
                    BigDecimal balance = rs.getBigDecimal("balance");
                    String accountNumber = rs.getString("account_number");
                    Account account = new Account(type, balance, accountNumber);
                    user.addAccount(account);
                }
            }

            if (user != null) {
                return Optional.of(user);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("유저 데이터를 DB에서 불러오기 중 오류 발생: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void saveToDB(User user) {
        String userSql = "INSERT INTO users (uuid, name, town_id, nation_id, created_at) " +
                "VALUES (?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE " +
                "name = ?, town_id = ?, nation_id = ?";
        String accountSql = "INSERT INTO accounts (account_number, owner_uuid, account_type, balance) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE balance = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(userSql)) {
                pstmt.setString(1, user.getUUID().toString());
                pstmt.setString(2, user.getName());
                pstmt.setLong(3, user.getTownID());
                pstmt.setLong(4, user.getNationID());
                pstmt.setString(5, user.getName());
                pstmt.setLong(6, user.getTownID());
                pstmt.setLong(7, user.getNationID());
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(accountSql)) {
                for (Account acc : user.getAccountList()) {
                    // if (acc.isDirty()) {
                    pstmt.setString(1, acc.getAccountNumber());
                    pstmt.setString(2, user.getUUID().toString());
                    pstmt.setInt(3, acc.getAccountType().getCode());
                    pstmt.setBigDecimal(4, acc.getBalance());
                    pstmt.setBigDecimal(5, acc.getBalance());
                    pstmt.addBatch();
                    // }
                    // acc.setDirty(false);
                }
                pstmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("유저 데이터를 DB에서 저장 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Optional<UUID>> findUUIDByAccountNumber(String accountNumber) {
        return CompletableFuture.supplyAsync(() -> {
           String sql = "SELECT owner_uuid FROM accounts WHERE account_number = ?";
           try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

               pstmt.setString(1, accountNumber);
               try (ResultSet rs = pstmt.executeQuery()) {
                   if (rs.next()) {
                       UUID uuid = UUID.fromString(rs.getString("owner_uuid"));
                       return Optional.of(uuid);
                   }
               }
           } catch (SQLException e) {
               plugin.getLogger().severe("계좌번호를 DB에서 로드 중 오류 발생: " + e.getMessage());
           }
           return Optional.empty();
        });
    }

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
}
