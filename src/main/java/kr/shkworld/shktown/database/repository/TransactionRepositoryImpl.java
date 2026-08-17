package kr.shkworld.shktown.database.repository;

import kr.shkworld.shktown.core.economy.model.Account;
import kr.shkworld.shktown.core.economy.model.AccountType;
import kr.shkworld.shktown.core.economy.model.TransactionReason;
import kr.shkworld.shktown.core.economy.model.TransactionFailure;
import kr.shkworld.shktown.core.economy.model.TransactionResult;
import kr.shkworld.shktown.core.economy.repository.TransactionRepository;
import kr.shkworld.shktown.database.DatabaseManager;
import kr.shkworld.shktown.database.UuidBinaryConverter;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class TransactionRepositoryImpl implements TransactionRepository {
    @Override
    public CompletableFuture<TransactionResult> transfer(
            UUID transactionId,
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            TransactionReason reason,
            String detail,
            String senderDisplay
    ) {
        if (transactionId == null || fromAccountNumber == null
                || toAccountNumber == null || reason == null) {
            return CompletableFuture.completedFuture(TransactionResult.failure(TransactionFailure.INVALID_REQUEST));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.completedFuture(TransactionResult.failure(TransactionFailure.INVALID_AMOUNT));
        }
        if (fromAccountNumber.equals(toAccountNumber)) {
            return CompletableFuture.completedFuture(TransactionResult.failure(TransactionFailure.SAME_ACCOUNT));
        }

        return CompletableFuture.supplyAsync(() -> transferSync(
                transactionId, fromAccountNumber, toAccountNumber,
                amount, reason, detail, senderDisplay
        ));
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(
            UUID transactionId,
            String accountNumber,
            BigDecimal amount,
            TransactionReason reason,
            String detail,
            String senderDisplay
    ) {
        if (transactionId == null || accountNumber == null || reason == null) {
            return CompletableFuture.completedFuture(TransactionResult.failure(TransactionFailure.INVALID_REQUEST));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.completedFuture(TransactionResult.failure(TransactionFailure.INVALID_AMOUNT));
        }

        return CompletableFuture.supplyAsync(() -> adjustSync(
                transactionId, null, accountNumber,
                amount, reason, detail, senderDisplay
        ));
    }

    @Override
    public CompletableFuture<TransactionResult> withdraw(
            UUID transactionId,
            String accountNumber,
            BigDecimal amount,
            TransactionReason reason,
            String detail,
            String senderDisplay
    ) {
        if (transactionId == null || accountNumber == null || reason == null) {
            return CompletableFuture.completedFuture(TransactionResult.failure(TransactionFailure.INVALID_REQUEST));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.completedFuture(TransactionResult.failure(TransactionFailure.INVALID_AMOUNT));
        }

        return CompletableFuture.supplyAsync(() -> adjustSync(
                transactionId, accountNumber, null,
                amount, reason, detail, senderDisplay
        ));
    }

    private TransactionResult transferSync(
            UUID transactionId,
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            TransactionReason reason,
            String detail,
            String senderDisplay
    ) {
        try (Connection connection = DatabaseManager.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                Map<String, LockedAccount> lockedAccounts = lockAccounts(
                        connection, fromAccountNumber, toAccountNumber
                );
                LockedAccount source = lockedAccounts.get(fromAccountNumber);
                LockedAccount target = lockedAccounts.get(toAccountNumber);

                if (source == null) {
                    connection.rollback();
                    return TransactionResult.failure(TransactionFailure.SOURCE_NOT_FOUND);
                }
                if (target == null) {
                    connection.rollback();
                    return TransactionResult.failure(TransactionFailure.TARGET_NOT_FOUND);
                }
                if (source.balance().compareTo(amount) < 0) {
                    connection.rollback();
                    return TransactionResult.failure(TransactionFailure.INSUFFICIENT_BALANCE);
                }

                BigDecimal sourceBalanceAfter = source.balance().subtract(amount);
                BigDecimal targetBalanceAfter = target.balance().add(amount);

                updateBalance(connection, fromAccountNumber, sourceBalanceAfter);
                updateBalance(connection, toAccountNumber, targetBalanceAfter);
                insertTransaction(
                        connection, transactionId, fromAccountNumber, toAccountNumber,
                        amount, reason, detail
                );
                insertEconomyLog(
                        connection, transactionId, source, fromAccountNumber,
                        senderDisplay, amount.negate(), sourceBalanceAfter, reason, detail
                );
                insertEconomyLog(
                        connection, transactionId, target, toAccountNumber,
                        senderDisplay, amount, targetBalanceAfter, reason, detail
                );

                connection.commit();
                return TransactionResult.success(
                        source.toAccount(fromAccountNumber, sourceBalanceAfter),
                        target.toAccount(toAccountNumber, targetBalanceAfter)
                );
            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new CompletionException("계좌이체 트랜잭션 중 DB 에러 발생: " + transactionId, e);
        }
    }

    private TransactionResult adjustSync(
            UUID transactionId,
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            TransactionReason reason,
            String detail,
            String senderDisplay
    ) {
        String accountNumber = fromAccountNumber != null ? fromAccountNumber : toAccountNumber;
        boolean withdraw = fromAccountNumber != null;

        try (Connection connection = DatabaseManager.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                LockedAccount account = lockAccount(connection, accountNumber);
                if (account == null) {
                    connection.rollback();
                    return TransactionResult.failure(withdraw
                            ? TransactionFailure.SOURCE_NOT_FOUND
                            : TransactionFailure.TARGET_NOT_FOUND);
                }
                if (withdraw && account.balance().compareTo(amount) < 0) {
                    connection.rollback();
                    return TransactionResult.failure(TransactionFailure.INSUFFICIENT_BALANCE);
                }

                BigDecimal signedAmount = withdraw ? amount.negate() : amount;
                BigDecimal balanceAfter = account.balance().add(signedAmount);

                updateBalance(connection, accountNumber, balanceAfter);
                insertTransaction(
                        connection, transactionId, fromAccountNumber, toAccountNumber,
                        amount, reason, detail
                );
                insertEconomyLog(
                        connection, transactionId, account, accountNumber,
                        senderDisplay, signedAmount, balanceAfter, reason, detail
                );

                connection.commit();
                Account updatedAccount = account.toAccount(accountNumber, balanceAfter);
                return withdraw
                        ? TransactionResult.success(updatedAccount, null)
                        : TransactionResult.success(null, updatedAccount);
            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new CompletionException("계좌 입출금 트랜잭션 중 DB 에러 발생: " + transactionId, e);
        }
    }

    private LockedAccount lockAccount(Connection connection, String accountNumber) throws SQLException {
        String sql = "SELECT owner_uuid, account_type, balance FROM accounts WHERE account_number = ? FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new LockedAccount(
                        UuidBinaryConverter.fromBytes(resultSet.getBytes("owner_uuid")),
                        AccountType.fromCode(resultSet.getInt("account_type")),
                        resultSet.getBigDecimal("balance")
                );
            }
        }
    }

    private Map<String, LockedAccount> lockAccounts(
            Connection connection, String fromAccountNumber, String toAccountNumber
    ) throws SQLException {
        String sql = "SELECT account_number, owner_uuid, account_type, balance " +
                "FROM accounts WHERE account_number IN (?, ?) ORDER BY account_number FOR UPDATE";
        Map<String, LockedAccount> accounts = new HashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fromAccountNumber);
            statement.setString(2, toAccountNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String accountNumber = resultSet.getString("account_number");
                    accounts.put(accountNumber, new LockedAccount(
                            UuidBinaryConverter.fromBytes(resultSet.getBytes("owner_uuid")),
                            AccountType.fromCode(resultSet.getInt("account_type")),
                            resultSet.getBigDecimal("balance")
                    ));
                }
            }
        }
        return accounts;
    }

    private void updateBalance(Connection connection, String accountNumber, BigDecimal balance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE accounts SET balance = ? WHERE account_number = ?")) {
            statement.setBigDecimal(1, balance);
            statement.setString(2, accountNumber);
            statement.executeUpdate();
        }
    }

    private void insertTransaction(
            Connection connection, UUID transactionId,
            String fromAccountNumber, String toAccountNumber,
            BigDecimal amount, TransactionReason reason, String detail
    ) throws SQLException {
        String sql = "INSERT INTO transactions " +
                "(uuid, from_account_number, to_account_number, amount, reason, detail, status, completed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'COMPLETED', CURRENT_TIMESTAMP(3))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinaryConverter.toBytes(transactionId));
            statement.setString(2, fromAccountNumber);
            statement.setString(3, toAccountNumber);
            statement.setBigDecimal(4, amount);
            statement.setString(5, reason.name());
            statement.setString(6, detail);
            statement.executeUpdate();
        }
    }

    private void insertEconomyLog(
            Connection connection, UUID transactionId, LockedAccount account,
            String accountNumber, String senderDisplay,
            BigDecimal amount, BigDecimal balanceAfter,
            TransactionReason reason, String detail
    ) throws SQLException {
        String sql = "INSERT INTO economy_logs " +
                "(transaction_id, asset_type, target_id, account_number, sender_display, amount, balance_after, reason, detail) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinaryConverter.toBytes(transactionId));
            statement.setInt(2, account.type().getCode());
            statement.setBytes(3, UuidBinaryConverter.toBytes(account.ownerUuid()));
            statement.setString(4, accountNumber);
            statement.setString(5, senderDisplay);
            statement.setBigDecimal(6, amount);
            statement.setBigDecimal(7, balanceAfter);
            statement.setString(8, reason.name());
            statement.setString(9, detail);
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

    private record LockedAccount(UUID ownerUuid, AccountType type, BigDecimal balance) {
        private Account toAccount(String accountNumber, BigDecimal updatedBalance) {
            return new Account(accountNumber, ownerUuid, type, updatedBalance);
        }
    }
}
