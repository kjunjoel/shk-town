package kr.shkworld.shktown.database;

import kr.shkworld.shktown.core.economy.model.Account;
import kr.shkworld.shktown.core.economy.model.AccountType;
import kr.shkworld.shktown.core.economy.model.CashReason;
import kr.shkworld.shktown.core.logging.model.LogType;
import kr.shkworld.shktown.core.economy.model.TransactionReason;
import kr.shkworld.shktown.core.economy.model.TransactionResult;
import kr.shkworld.shktown.core.economy.model.User;
import kr.shkworld.shktown.core.logging.service.LogService;
import kr.shkworld.shktown.core.economy.service.impl.UserServiceImpl;
import kr.shkworld.shktown.database.repository.AccountRepositoryImpl;
import kr.shkworld.shktown.database.repository.TransactionRepositoryImpl;
import kr.shkworld.shktown.database.repository.UserRepositoryImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryIntegrationTest {
    private static final UserRepositoryImpl USER_REPOSITORY = new UserRepositoryImpl();
    private static final AccountRepositoryImpl ACCOUNT_REPOSITORY = new AccountRepositoryImpl();
    private static final TransactionRepositoryImpl TRANSACTION_REPOSITORY = new TransactionRepositoryImpl();

    @BeforeAll
    static void connect() throws Exception {
        Map<String, String> config = readDatabaseConfig();
        DatabaseManager.getInstance().setup(
                config.get("host"), Integer.parseInt(config.get("port")),
                config.get("database"), config.get("username"), config.get("password")
        );
    }

    @AfterAll
    static void disconnect() {
        DatabaseManager.getInstance().close();
    }

    @Test
    void savesAndLoadsUserProfileWithoutOverwritingCashOnUpdate() throws Exception {
        UUID uuid = UUID.randomUUID();
        try {
            User user = new User(uuid, "repo_test", new BigDecimal("100.00"),
                    LocalDateTime.now(), LocalDateTime.now());
            USER_REPOSITORY.saveUserSync(user);
            assertEquals(new BigDecimal("100.00"), USER_REPOSITORY.loadUserSync(uuid).orElseThrow().getCash());

            User profileUpdate = new User(uuid, "repo_updated", new BigDecimal("999.00"),
                    LocalDateTime.now(), user.getFirstLogin());
            USER_REPOSITORY.saveUserAsync(profileUpdate).join();
            assertEquals(new BigDecimal("100.00"), USER_REPOSITORY.loadUserAsync(uuid).join().orElseThrow().getCash());
        } finally {
            deleteUser(uuid);
        }
    }

    @Test
    void addsCashAndWritesLogInSameRepositoryOperation() throws Exception {
        UUID uuid = UUID.randomUUID();
        try {
            USER_REPOSITORY.saveUserSync(new User(uuid, "cash_add"));

            User updated = USER_REPOSITORY.addCashAsync(
                    uuid, new BigDecimal("150.00"), CashReason.ADMIN_ADJUST, "integration cash add"
            ).join().orElseThrow();

            assertEquals(new BigDecimal("150.00"), updated.getCash());
            assertEquals(new BigDecimal("150.00"), USER_REPOSITORY.loadUserSync(uuid).orElseThrow().getCash());
            assertEquals(1, count("cash_logs", "user_uuid", uuid));
        } finally {
            deleteCashLogs(uuid);
            deleteUser(uuid);
        }
    }

    @Test
    void subtractsCashAndWritesLogInSameRepositoryOperation() throws Exception {
        UUID uuid = UUID.randomUUID();
        try {
            User user = new User(uuid, "cash_subtract", new BigDecimal("200.00"),
                    LocalDateTime.now(), LocalDateTime.now());
            USER_REPOSITORY.saveUserSync(user);

            User updated = USER_REPOSITORY.subtractCashAsync(
                    uuid, new BigDecimal("75.00"), CashReason.ADMIN_ADJUST, "integration cash subtract"
            ).join().orElseThrow();

            assertEquals(new BigDecimal("125.00"), updated.getCash());
            assertEquals(new BigDecimal("125.00"), USER_REPOSITORY.loadUserSync(uuid).orElseThrow().getCash());
            assertEquals(1, count("cash_logs", "user_uuid", uuid));
        } finally {
            deleteCashLogs(uuid);
            deleteUser(uuid);
        }
    }

    @Test
    void doesNotSubtractCashOrWriteLogWhenBalanceIsInsufficient() throws Exception {
        UUID uuid = UUID.randomUUID();
        try {
            User user = new User(uuid, "cash_fail", new BigDecimal("50.00"),
                    LocalDateTime.now(), LocalDateTime.now());
            USER_REPOSITORY.saveUserSync(user);

            assertTrue(USER_REPOSITORY.subtractCashAsync(
                    uuid, new BigDecimal("75.00"), CashReason.ADMIN_ADJUST, "integration cash fail"
            ).join().isEmpty());

            assertEquals(new BigDecimal("50.00"), USER_REPOSITORY.loadUserSync(uuid).orElseThrow().getCash());
            assertEquals(0, count("cash_logs", "user_uuid", uuid));
        } finally {
            deleteCashLogs(uuid);
            deleteUser(uuid);
        }
    }

    @Test
    void serializesConcurrentCashChangesAndKeepsServiceCacheFresh() throws Exception {
        UUID uuid = UUID.randomUUID();
        UserServiceImpl userService = new UserServiceImpl(USER_REPOSITORY, NOOP_LOG_SERVICE);
        try {
            User user = new User(uuid, "cash_queue", new BigDecimal("100.00"),
                    LocalDateTime.now(), LocalDateTime.now());
            USER_REPOSITORY.saveUserSync(user);
            userService.prepareUserAsync(uuid, "cash_queue").join();

            CompletableFuture<?>[] purchases = new CompletableFuture<?>[10];
            for (int i = 0; i < purchases.length; i++) {
                purchases[i] = userService.subtractCashAsync(
                        uuid, new BigDecimal("10.00"), CashReason.SHOP_PURCHASE, "concurrent purchase"
                );
            }

            CompletableFuture.allOf(purchases).join();

            assertEquals(new BigDecimal("0.00"), USER_REPOSITORY.loadUserSync(uuid).orElseThrow().getCash());
            assertEquals(new BigDecimal("0.00"), userService.getUserSync(uuid).orElseThrow().getCash());
            assertEquals(10, count("cash_logs", "user_uuid", uuid));
        } finally {
            deleteCashLogs(uuid);
            deleteUser(uuid);
        }
    }

    @Test
    void savesLoadsAndDeletesAccountWithoutDeltaTracking() throws Exception {
        UUID ownerUuid = UUID.randomUUID();
        String accountNumber = accountNumber("10");
        Account account = new Account(accountNumber, ownerUuid, AccountType.SAVINGS, new BigDecimal("500.00"));
        try {
            ACCOUNT_REPOSITORY.saveAccountSync(account);
            assertEquals(new BigDecimal("500.00"),
                    ACCOUNT_REPOSITORY.loadAccountByNumberSync(accountNumber).orElseThrow().getBalance());
            assertEquals(1, ACCOUNT_REPOSITORY.loadAccountByOwnerAsync(ownerUuid).join().size());

            ACCOUNT_REPOSITORY.deleteAccount(accountNumber).join();
            assertTrue(ACCOUNT_REPOSITORY.loadAccountByNumberAsync(accountNumber).join().isEmpty());
        } finally {
            deleteAccount(accountNumber);
        }
    }

    @Test
    void transfersMoneyAndWritesTransactionAndTwoLogs() throws Exception {
        TransferFixture fixture = createTransferFixture(new BigDecimal("1000.00"));
        UUID transactionId = UUID.randomUUID();
        try {
            TransactionResult result = TRANSACTION_REPOSITORY.transfer(
                    transactionId, fixture.source(), fixture.target(),
                    new BigDecimal("250.00"), TransactionReason.TRANSFER,
                    "integration transfer", "repo_test"
            ).join();

            assertTrue(result.success());
            assertEquals(new BigDecimal("750.00"), balance(fixture.source()));
            assertEquals(new BigDecimal("250.00"), balance(fixture.target()));
            assertEquals(1, count("transactions", "uuid", transactionId));
            assertEquals(2, count("economy_logs", "transaction_id", transactionId));
        } finally {
            deleteTransferFixture(transactionId, fixture);
        }
    }

    @Test
    void depositsMoneyAndWritesTransactionAndLog() throws Exception {
        String accountNumber = accountNumber("10");
        UUID transactionId = UUID.randomUUID();
        try {
            ACCOUNT_REPOSITORY.saveAccountSync(new Account(
                    accountNumber, UUID.randomUUID(), AccountType.SAVINGS, BigDecimal.ZERO));

            TransactionResult result = TRANSACTION_REPOSITORY.deposit(
                    transactionId, accountNumber, new BigDecimal("300.00"),
                    TransactionReason.ADMIN_ADJUST, "integration deposit", "repo_test"
            ).join();

            assertTrue(result.success());
            assertEquals(new BigDecimal("300.00"), result.targetAccount().getBalance());
            assertEquals(new BigDecimal("300.00"), balance(accountNumber));
            assertEquals(1, count("transactions", "uuid", transactionId));
            assertEquals(1, count("economy_logs", "transaction_id", transactionId));
        } finally {
            execute("DELETE FROM economy_logs WHERE transaction_id = ?", transactionId);
            execute("DELETE FROM transactions WHERE uuid = ?", transactionId);
            deleteAccount(accountNumber);
        }
    }

    @Test
    void withdrawsMoneyAndWritesTransactionAndLog() throws Exception {
        String accountNumber = accountNumber("10");
        UUID transactionId = UUID.randomUUID();
        try {
            ACCOUNT_REPOSITORY.saveAccountSync(new Account(
                    accountNumber, UUID.randomUUID(), AccountType.SAVINGS, new BigDecimal("500.00")));

            TransactionResult result = TRANSACTION_REPOSITORY.withdraw(
                    transactionId, accountNumber, new BigDecimal("125.00"),
                    TransactionReason.ADMIN_ADJUST, "integration withdraw", "repo_test"
            ).join();

            assertTrue(result.success());
            assertEquals(new BigDecimal("375.00"), result.sourceAccount().getBalance());
            assertEquals(new BigDecimal("375.00"), balance(accountNumber));
            assertEquals(1, count("transactions", "uuid", transactionId));
            assertEquals(1, count("economy_logs", "transaction_id", transactionId));
        } finally {
            execute("DELETE FROM economy_logs WHERE transaction_id = ?", transactionId);
            execute("DELETE FROM transactions WHERE uuid = ?", transactionId);
            deleteAccount(accountNumber);
        }
    }

    @Test
    void rollsBackBalancesWhenEconomyLogInsertFails() throws Exception {
        TransferFixture fixture = createTransferFixture(new BigDecimal("1000.00"));
        UUID transactionId = UUID.randomUUID();
        try {
            String tooLongSenderDisplay = "x".repeat(33);
            assertThrows(CompletionException.class, () ->
                    TRANSACTION_REPOSITORY.transfer(
                            transactionId, fixture.source(), fixture.target(),
                            new BigDecimal("250.00"), TransactionReason.TRANSFER,
                            "rollback test", tooLongSenderDisplay
                    ).join());

            assertEquals(new BigDecimal("1000.00"), balance(fixture.source()));
            assertEquals(new BigDecimal("0.00"), balance(fixture.target()));
            assertEquals(0, count("transactions", "uuid", transactionId));
            assertEquals(0, count("economy_logs", "transaction_id", transactionId));
        } finally {
            deleteTransferFixture(transactionId, fixture);
        }
    }

    private static TransferFixture createTransferFixture(BigDecimal sourceBalance) throws Exception {
        String source = accountNumber("10");
        String target = accountNumber("11");
        ACCOUNT_REPOSITORY.saveAccountSync(new Account(
                source, UUID.randomUUID(), AccountType.SAVINGS, sourceBalance));
        ACCOUNT_REPOSITORY.saveAccountSync(new Account(
                target, UUID.randomUUID(), AccountType.INVESTMENT, BigDecimal.ZERO));
        return new TransferFixture(source, target);
    }

    private static BigDecimal balance(String accountNumber) throws Exception {
        return ACCOUNT_REPOSITORY.loadAccountByNumberSync(accountNumber).orElseThrow().getBalance();
    }

    private static int count(String table, String column, UUID uuid) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (Connection connection = DatabaseManager.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinaryConverter.toBytes(uuid));
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static void deleteTransferFixture(UUID transactionId, TransferFixture fixture) throws Exception {
        execute("DELETE FROM economy_logs WHERE transaction_id = ?", transactionId);
        execute("DELETE FROM transactions WHERE uuid = ?", transactionId);
        deleteAccount(fixture.source());
        deleteAccount(fixture.target());
    }

    private static void deleteAccount(String accountNumber) throws Exception {
        try (Connection connection = DatabaseManager.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM accounts WHERE account_number = ?")) {
            statement.setString(1, accountNumber);
            statement.executeUpdate();
        }
    }

    private static void deleteUser(UUID uuid) throws Exception {
        execute("DELETE FROM users WHERE uuid = ?", uuid);
    }

    private static void deleteCashLogs(UUID uuid) throws Exception {
        execute("DELETE FROM cash_logs WHERE user_uuid = ?", uuid);
    }

    private static void execute(String sql, UUID uuid) throws Exception {
        try (Connection connection = DatabaseManager.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, UuidBinaryConverter.toBytes(uuid));
            statement.executeUpdate();
        }
    }

    private static String accountNumber(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 16);
    }

    private static Map<String, String> readDatabaseConfig() throws Exception {
        Map<String, String> config = new HashMap<>();
        boolean databaseSection = false;
        for (String raw : Files.readAllLines(Path.of("src/main/resources/config.yml"))) {
            if (raw.equals("database:")) { databaseSection = true; continue; }
            if (databaseSection && !raw.startsWith("  ")) break;
            if (!databaseSection) continue;
            String line = raw.trim();
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            config.put(line.substring(0, colon), line.substring(colon + 1).trim().replaceAll("^\"|\"$", ""));
        }
        return config;
    }

    private record TransferFixture(String source, String target) {
    }

    private static final LogService NOOP_LOG_SERVICE = new LogService() {
        @Override
        public CompletableFuture<Void> logSystem(LogType logType, String message, Map<String, Object> data) {
            return CompletableFuture.completedFuture(null);
        }
    };
}
