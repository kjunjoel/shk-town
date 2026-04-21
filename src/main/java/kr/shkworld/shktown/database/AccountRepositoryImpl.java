package kr.shkworld.shktown.database;

import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.repository.AccountRepository;
import org.bukkit.plugin.java.JavaPlugin;

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

public class AccountRepositoryImpl implements AccountRepository {
    private final JavaPlugin plugin;

    public AccountRepositoryImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<Void> saveAccount(Account account) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveAccountSync(account);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    @Override
    public void saveAccountSync(Account account) {
        String sql = "INSERT INTO accounts (account_number, owner_uuid, account_type, balance) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "balance = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, account.getAccountNumber());
            preparedStatement.setString(2, account.getOwnerUUID().toString());
            preparedStatement.setInt(3, account.getAccountType().getCode());
            preparedStatement.setBigDecimal(4, account.getBalance());
            preparedStatement.setBigDecimal(5, account.getBalance());
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("DB에 UUID " + account.getOwnerUUID().toString() + "님의 계좌번호 " + account.getAccountNumber() + "의 데이터를 저장하던 중 오류가 발생하였습니다.\n" + e.getMessage());
            throw new RuntimeException(e);

        }
    }

    @Override
    public CompletableFuture<Optional<Account>> findByNumber(String accountNumber) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT owner_uuid, account_type, balance " +
                         "FROM accounts " +
                         "WHERE account_number = ?";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, accountNumber);

                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        return Optional.of(new Account(
                                rs.getObject("owner_uuid", UUID.class),
                                AccountType.fromCode(rs.getInt("account_type")),
                                accountNumber,
                                rs.getBigDecimal("balance")
                        ));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 계좌번호 " + accountNumber + "의 데이터를 불러오던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<List<Account>> findAllByOwner(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Account> accounts = new ArrayList<>();
            String sql = "SELECT account_number, account_type, balance " +
                         "FROM accounts " +
                         "WHERE owner_uuid = ?";

            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, uuid.toString());

                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        accounts.add(new Account(
                                uuid,
                                AccountType.fromCode(rs.getInt("account_type")),
                                rs.getString("account_number"),
                                rs.getBigDecimal("balance")
                        ));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("DB에서 UUID " + uuid.toString() + "의 데이터를 불러오던 중 오류가 발생하였습니다.\n" + e.getMessage());
                throw new CompletionException(e);

            }
            return accounts;
        });
    }
}
