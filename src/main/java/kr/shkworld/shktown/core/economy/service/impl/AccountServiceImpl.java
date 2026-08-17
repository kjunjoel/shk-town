package kr.shkworld.shktown.core.economy.service.impl;

import kr.shkworld.shktown.core.economy.model.Account;
import kr.shkworld.shktown.core.economy.model.AccountType;
import kr.shkworld.shktown.core.logging.model.LogType;
import kr.shkworld.shktown.core.economy.model.TransactionReason;
import kr.shkworld.shktown.core.economy.repository.AccountRepository;
import kr.shkworld.shktown.core.economy.repository.TransactionRepository;
import kr.shkworld.shktown.core.economy.service.AccountService;
import kr.shkworld.shktown.core.logging.service.LogService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LogService logService;

    private final Map<String, Account> accountsByNum = new ConcurrentHashMap<>();
    private final Map<OwnerAccountKey, String> accountKeysByOwner = new ConcurrentHashMap<>();

    public AccountServiceImpl(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LogService logService
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.logService = logService;
    }

    @Override
    public Optional<Account> getAccountSync(String accountNumber) {
        return Optional.ofNullable(accountsByNum.get(accountNumber));
    }

    @Override
    public CompletableFuture<Optional<Account>> getAccountAsync(String accountNumber) {
        Account cached = accountsByNum.get(accountNumber);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return accountRepository.loadAccountByNumberAsync(accountNumber)
                .thenApply(accountOpt -> {
                    accountOpt.ifPresent(this::loadAccount);
                    return accountOpt;
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        logService.logSystem(
                            LogType.ERROR,
                            "계좌번호로 Account 비동기 불러오기 중 DB 예외 발생",
                            Map.of("account_number", accountNumber, "error", throwable.getMessage())
                        );
                    }
                });
    };

    @Override
    public CompletableFuture<Optional<Account>> refreshAccountAsync(String accountNumber) {
        return accountRepository.loadAccountByNumberAsync(accountNumber)
                .thenApply(accountOpt -> {
                    accountOpt.ifPresentOrElse(
                            this::loadAccount,
                            () -> unloadAccount(accountNumber)
                    );
                    return accountOpt;
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        logService.logSystem(
                            LogType.ERROR,
                            "계좌 캐시 새로고침 중 DB 예외 발생",
                            Map.of("account_number", accountNumber, "error", throwable.getMessage())
                        );
                    }
                });
    }

    @Override
    public Optional<Account> getAccountByOwnerSync(UUID ownerUuid, AccountType accountType) {
        String accountNumber = accountKeysByOwner.get(new OwnerAccountKey(ownerUuid, accountType));
        if (accountNumber == null) return Optional.empty();

        return getAccountSync(accountNumber);
    }

    @Override
    public CompletableFuture<Optional<Account>> getAccountByOwnerAsync(UUID ownerUuid, AccountType accountType) {
        String accountNumber = accountKeysByOwner.get(new OwnerAccountKey(ownerUuid, accountType));

        if (accountNumber != null) {
            Account cached = accountsByNum.get(accountNumber);
            if (cached != null) {
                return CompletableFuture.completedFuture(Optional.of(cached));
            }
        }

        return accountRepository.loadAccountByOwnerAsync(ownerUuid)
                .thenApply(accList -> {
                    accList.forEach(this::loadAccount);
                    return accList.stream()
                            .filter(acc -> acc.getAccountType() == accountType)
                            .findFirst();
                }).whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        logService.logSystem(
                            LogType.ERROR,
                            "UUID와 계좌 유형으로 Account 비동기 불러오기 중 DB 예외 발생",
                            Map.of("owner_uuid", ownerUuid.toString(), "account_type", accountType.getCode(), "error", throwable.getMessage())
                        );
                    }
                });
    }

    private Collection<Account> getAllAccounts(List<AccountType> accountTypes) {
        return accountsByNum.values().stream()
            .filter(acc -> accountTypes.contains(acc.getAccountType()))
            .toList();
    }

    private void loadAccount(Account account) {
        OwnerAccountKey indexKey = new OwnerAccountKey(account.getOwnerUuid(), account.getAccountType());
        String oldAccountNumber = accountKeysByOwner.get(indexKey);
        
        if (oldAccountNumber != null && !oldAccountNumber.equals(account.getAccountNumber())) {
            accountsByNum.remove(oldAccountNumber);
        }

        accountsByNum.put(account.getAccountNumber(), account);
        accountKeysByOwner.put(indexKey, account.getAccountNumber());
    }

    private void unloadAccount(String accountNumber) {
        Account account = accountsByNum.remove(accountNumber);
        
        if (account != null) {
            OwnerAccountKey indexKey = new OwnerAccountKey(account.getOwnerUuid(), account.getAccountType());
            accountKeysByOwner.remove(indexKey);
        }
    }

    private void saveAccountSync(Account account) {
        try {
            accountRepository.saveAccountSync(account);
        } catch (Exception e) {
            logService.logSystem(
                LogType.ERROR,
                "Account 동기 저장 중 DB 예외 발생",
                Map.of("account_number", account.getAccountNumber(), "error", e.getMessage())
            );
        }
    }

    private CompletableFuture<Void> saveAccountAsync(Account account) {
        return accountRepository.saveAccountAsync(account)
                .whenComplete((ignored, throwable) -> {
                    if (throwable == null) {
                        return;
                    }
                    logService.logSystem(
                        LogType.ERROR,
                        "Account 비동기 저장 중 DB 예외 발생",
                        Map.of("account_number", account.getAccountNumber(), "error", throwable.getMessage())
                    );
                });
    }

    @Override
    public CompletableFuture<Account> createAccount(UUID ownerUuid, AccountType accountType) {
        return CompletableFuture.supplyAsync(() -> {
            int typeCode = accountType.getCode();
            long timePart = (System.currentTimeMillis() / 1_000L) % 1_000_000L;
            int uuidPart = Math.abs(ownerUuid.hashCode() % 1_000);

            String combinedStr = String.format("%02d%06d%03d", typeCode, timePart, uuidPart);
            BigInteger bigNumber = new BigInteger(combinedStr);
            BigInteger remainder = bigNumber.mod(BigInteger.valueOf(97));
            int checkDigit = 98 - remainder.intValue();;

            String accountNumber = String.format("%02d-%06d-%03d-%02d", typeCode, timePart, uuidPart, checkDigit);
            Account account = new Account(accountNumber, ownerUuid, accountType, BigDecimal.ZERO);
            loadAccount(account);

            return account;
        }).thenCompose(account -> saveAccountAsync(account).thenApply(v -> account));
    }

    @Override
    public CompletableFuture<Account> ensureAccountAsync(UUID ownerUuid, AccountType accountType) {
        return getAccountByOwnerAsync(ownerUuid, accountType)
                .thenCompose(accountOpt -> accountOpt
                        .map(CompletableFuture::completedFuture)
                        .orElseGet(() -> createAccount(ownerUuid, accountType)));
    }

    @Override
    public CompletableFuture<Void> deleteAccount(String accountNumber) {
        Account removed = accountsByNum.get(accountNumber);
        return accountRepository.deleteAccount(accountNumber)
                .whenComplete((ignored, throwable) -> {
                    if (throwable == null) {
                        unloadAccount(accountNumber);
                        return;
                    }
                    if (removed != null) {
                        loadAccount(removed);
                    }
                    logService.logSystem(
                        LogType.ERROR,
                        "Account 비동기 삭제 중 DB 예외 발생",
                        Map.of("account_number", accountNumber, "error", throwable.getMessage())
                    );
                });
    }

    @Override
    public CompletableFuture<Void> flushAndUnloadOwnerAccountsAsync(UUID ownerUuid) {
        CompletableFuture<?>[] accountFlushes = accountsByNum.values().stream()
                .filter(account -> account.getOwnerUuid().equals(ownerUuid))
                .map(account -> saveAccountAsync(account)
                        .thenRun(() -> unloadAccount(account.getAccountNumber())))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(accountFlushes);
    }

    @Override
    public void flushAllSync() {
        accountsByNum.values().forEach(this::saveAccountSync);
    }

    @Override
    public void depositSync(String accountNumber, BigDecimal amount, TransactionReason reason, String detail) {
        depositAsync(accountNumber, amount, reason, detail).join();
    }

    @Override
    public CompletableFuture<Void> depositAsync(String accountNumber, BigDecimal amount, TransactionReason reason, String detail) {
        return transactionRepository.deposit(
                UUID.randomUUID(), accountNumber, amount, reason, detail, null
        ).thenCompose(result -> {
            if (!result.success()) {
                return CompletableFuture.completedFuture(null);
            }
            return refreshAccountAsync(accountNumber).thenApply(ignored -> null);
        });
    }

    @Override
    public boolean withdrawSync(String accountNumber, BigDecimal amount, TransactionReason reason, String detail) {
        return withdrawAsync(accountNumber, amount, reason, detail).join();
    }

    @Override
    public CompletableFuture<Boolean> withdrawAsync(String accountNumber, BigDecimal amount, TransactionReason reason, String detail) {
        return transactionRepository.withdraw(
                UUID.randomUUID(), accountNumber, amount, reason, detail, null
        ).thenCompose(result -> {
            if (!result.success()) {
                return CompletableFuture.completedFuture(false);
            }
            return refreshAccountAsync(accountNumber).thenApply(ignored -> true);
        });
    }

    public record OwnerAccountKey(UUID ownerUuid, AccountType accountType) {}
}
