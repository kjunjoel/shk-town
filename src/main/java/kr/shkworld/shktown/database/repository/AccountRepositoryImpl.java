package kr.shkworld.shktown.database.repository;

import kr.shkworld.shktown.core.economy.model.Account;
import kr.shkworld.shktown.core.economy.model.AccountType;
import kr.shkworld.shktown.core.economy.repository.AccountRepository;
import kr.shkworld.shktown.database.DatabaseManager;
import kr.shkworld.shktown.database.UuidBinaryConverter;

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

    @Override
    public void saveAccountSync(Account account) throws SQLException {
        synchronized (account) {
            String sql = "INSERT INTO accounts (account_number, owner_uuid, account_type, balance) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE owner_uuid = VALUES(owner_uuid), account_type = VALUES(account_type)";

            try (Connection connection = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, account.getAccountNumber());
                preparedStatement.setBytes(2, UuidBinaryConverter.toBytes(account.getOwnerUuid()));
                preparedStatement.setInt(3, account.getAccountType().getCode());
                preparedStatement.setBigDecimal(4, account.getBalance());
                preparedStatement.executeUpdate();
            }
        }
    }

    @Override
    public CompletableFuture<Void> saveAccountAsync(Account account) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveAccountSync(account);
            } catch (SQLException e) {
                throw new CompletionException("Account 비동기 저장 중 DB 에러 발생: " + account.getAccountNumber(), e);
            }
        });
    }

    @Override
    public Optional<Account> loadAccountByNumberSync(String accountNumber) throws SQLException {
        String sql = "SELECT owner_uuid, account_type, balance FROM accounts WHERE account_number = ?";

        try (Connection connection = DatabaseManager.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, accountNumber);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(toAccount(resultSet, accountNumber));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Optional<Account>> loadAccountByNumberAsync(String accountNumber) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadAccountByNumberSync(accountNumber);
            } catch (SQLException e) {
                throw new CompletionException("계좌번호로 Account 비동기 조회 중 DB 에러 발생: " + accountNumber, e);
            }
        });
    }

    @Override
    public List<Account> loadAccountByOwnerSync(UUID ownerUuid) throws SQLException {
        String sql = "SELECT account_number, account_type, balance FROM accounts WHERE owner_uuid = ?";
        List<Account> accounts = new ArrayList<>();

        try (Connection connection = DatabaseManager.getInstance().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setBytes(1, UuidBinaryConverter.toBytes(ownerUuid));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.add(new Account(
                            resultSet.getString("account_number"),
                            ownerUuid,
                            AccountType.fromCode(resultSet.getInt("account_type")),
                            resultSet.getBigDecimal("balance")
                    ));
                }
            }
        }
        return accounts;
    }

    @Override
    public CompletableFuture<List<Account>> loadAccountByOwnerAsync(UUID ownerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadAccountByOwnerSync(ownerUuid);
            } catch (SQLException e) {
                throw new CompletionException("소유자 UUID로 Account 비동기 조회 중 DB 에러 발생: " + ownerUuid, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteAccount(String accountNumber) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM accounts WHERE account_number = ?";
            try (Connection connection = DatabaseManager.getInstance().getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, accountNumber);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new CompletionException("Account 비동기 삭제 중 DB 에러 발생: " + accountNumber, e);
            }
        });
    }

    private Account toAccount(ResultSet resultSet, String accountNumber) throws SQLException {
        return new Account(
                accountNumber,
                UuidBinaryConverter.fromBytes(resultSet.getBytes("owner_uuid")),
                AccountType.fromCode(resultSet.getInt("account_type")),
                resultSet.getBigDecimal("balance")
        );
    }
}
