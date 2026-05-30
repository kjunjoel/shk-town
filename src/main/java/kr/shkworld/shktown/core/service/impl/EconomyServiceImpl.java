package kr.shkworld.shktown.core.service.impl;

import kr.shkworld.shktown.core.economy.wealth.WealthComponent;
import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.CashReason;
import kr.shkworld.shktown.core.model.LogType;
import kr.shkworld.shktown.core.model.RankEntry;
import kr.shkworld.shktown.core.model.TransactionReason;
import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.repository.EconomyRepository;
import kr.shkworld.shktown.core.service.AccountService;
import kr.shkworld.shktown.core.service.EconomyService;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.service.UserService;
import kr.shkworld.util.PluginLogger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyServiceImpl implements EconomyService {
    private final EconomyRepository economyRepository;
    private final UserService userService;
    private final AccountService accountService;
    private final LogService logService;
    private final PluginLogger pluginLogger;

    private final List<WealthComponent> wealthComponents = new ArrayList<>();

    public EconomyServiceImpl(EconomyRepository economyRepository, UserService userService, AccountService accountService, LogService logService, PluginLogger pluginLogger) {
        this.economyRepository = economyRepository;
        this.userService = userService;
        this.accountService = accountService;
        this.logService = logService;
        this.pluginLogger = pluginLogger;
    }

    @Override
    public CompletableFuture<Boolean> transfer(
            UUID fromOwner, AccountType fromType,
            UUID toOwner, AccountType toType,
            BigDecimal amount, TransactionReason reason, String detail
    ) {
        CompletableFuture<List<Account>> fromAccFuture = accountService.getAccountsByOwner(fromOwner);
        CompletableFuture<List<Account>> toAccFuture = accountService.getAccountsByOwner(toOwner);

        return fromAccFuture.thenCombine(toAccFuture, (fromList, toList) -> {
            Optional<Account> fromAcc = fromList.stream().filter(acc -> acc.getAccountType() == fromType).findFirst();
            Optional<Account> toAcc = toList.stream().filter(acc -> acc.getAccountType() == toType).findFirst();

            if (fromAcc.isEmpty() || toAcc.isEmpty()) return CompletableFuture.completedFuture(false);

            return transferByAccountNumber(
                    fromAcc.get().getAccountNumber(),
                    toAcc.get().getAccountNumber(),
                    amount, reason, detail
            );
        }).thenCompose(future -> future);
    }

    @Override
    public CompletableFuture<Boolean> transferByAccountNumber(String fromAccountNumber, String toAccountNumber, BigDecimal amount, TransactionReason reason, String detail) {
        String transferID = UUID.randomUUID().toString().substring(0, 8);

        CompletableFuture<Optional<Account>> fromAccFuture = accountService.getAccount(fromAccountNumber);
        CompletableFuture<Optional<Account>> toAccFuture = accountService.getAccount(toAccountNumber);

        return fromAccFuture.thenCombine(toAccFuture, (fromAccOpt, toAccOpt) -> {
            if (fromAccOpt.isEmpty() || toAccOpt.isEmpty()) return CompletableFuture.completedFuture(false);

            Account fromAcc = fromAccOpt.get();
            Account toAcc = toAccOpt.get();

            return accountService.withdraw(fromAccountNumber, amount, reason, detail).thenCompose(withdrawSuccess -> {
                if (!withdrawSuccess) return CompletableFuture.completedFuture(false);

                return accountService.deposit(toAccountNumber, amount, reason, detail).thenCompose(depositSuccess -> {
                    if(!depositSuccess) {
                        return accountService.deposit(fromAccountNumber, amount, reason, "[복구] 송금 실패: " + transferID)
                                .thenApply(v -> false);
                    }

                    String syncDetail = String.format("[%s] %s", transferID, detail);

                    CompletableFuture<Void> logFrom = logService.logEconomy(
                            fromAcc.getAccountType(), fromAcc.getOwnerUUID().toString(), fromAccountNumber,
                            amount.negate(), fromAcc.getBalance(), reason, "To: " + toAccountNumber + " | " + syncDetail
                    );

                    CompletableFuture<Void> logTo = logService.logEconomy(
                           toAcc.getAccountType(), toAcc.getOwnerUUID().toString(), toAccountNumber,
                           amount, toAcc.getBalance(), reason, "From: " + fromAccountNumber + " | " + syncDetail
                    );

                    return CompletableFuture.allOf(logFrom, logTo).thenApply(v -> true);
                });
            });
        }).thenCompose(future -> future).exceptionally(ex -> {
            Map<String, Object> errorData = new ConcurrentHashMap<>();
            errorData.put("transferID", transferID);
            errorData.put("from", fromAccountNumber);
            errorData.put("to", toAccountNumber);
            errorData.put("errorType", ex.getClass().getSimpleName());
            errorData.put("errorMessage", ex.getMessage());

            logService.logSystem(LogType.ERROR, "송금 실패: " + transferID, errorData);
            pluginLogger.severe(String.format("[%s] 송금 실패: %s", transferID, ex.getMessage()));
            return true;
        });
    }

    @Override
    public CompletableFuture<Boolean> updateBalance(UUID owner, AccountType type, BigDecimal amount, TransactionReason reason, String detail) {
        return accountService.getAccountsByOwner(owner).thenCompose(accounts -> {
            Optional<Account> targetAcc = accounts.stream().filter(acc -> acc.getAccountType() == type).findFirst();

            if (targetAcc.isEmpty()) return CompletableFuture.completedFuture(false);
            Account account = targetAcc.get();
            String accountNumber = account.getAccountNumber();

            CompletableFuture<Boolean> action = (amount.signum() >= 0)
                    ? accountService.deposit(accountNumber, amount, reason, detail)
                    : accountService.withdraw(accountNumber, amount.abs(), reason, detail);

            return action.thenCompose(success -> {
                if (!success) return CompletableFuture.completedFuture(false);

                BigDecimal balanceAfter = account.getBalance();
                return logService.logEconomy(type, owner.toString(), accountNumber, amount, balanceAfter, reason, detail)
                        .thenApply(v -> true)
                        .exceptionally(ex -> {
                            Map<String, Object> errorData = new ConcurrentHashMap<>();
                            errorData.put("owner", owner.toString());
                            errorData.put("accountNumber", accountNumber);
                            errorData.put("amount", amount);
                            errorData.put("errorType", ex.getClass().getSimpleName());
                            errorData.put("errorMessage", ex.getMessage());

                            logService.logSystem(LogType.ERROR, "잔액 변경 실패: " + accountNumber, errorData);
                            pluginLogger.severe(String.format("[%s] 잔액 변경 실패: %s", accountNumber, ex.getMessage()));
                            return true;
                        });
            });
        });
    }

    @Override
    public CompletableFuture<Boolean> updateCash(UUID uuid, BigDecimal amount, CashReason reason, String detail) {
        return userService.getUserAsync(uuid).thenCompose(userOpt -> {
            if (userOpt.isEmpty()) return CompletableFuture.completedFuture(false);

            User user = userOpt.get();
            user.addCash(amount);

            return userService.saveUser(user).thenCompose(v -> logService.logEconomy(
                    AccountType.CASH, uuid.toString(), null, amount, user.getCash(), reason, detail)
                    .thenApply(unused -> true))
                    .exceptionally(ex -> {
                        Map<String, Object> errorData = new ConcurrentHashMap<>();
                        errorData.put("uuid", uuid.toString());
                        errorData.put("amount", amount);
                        errorData.put("errorType", ex.getClass().getSimpleName());
                        errorData.put("errorMessage", ex.getMessage());

                        logService.logSystem(LogType.ERROR, "캐시 변경 실패: " + uuid, errorData);
                        pluginLogger.severe(String.format("[%s] 캐시 변경 실패: %s", uuid, ex.getMessage()));
                        return true;
                    });
        });
    }

    @Override
    public CompletableFuture<BigDecimal> getTotalWealth(UUID uuid) {
        List<CompletableFuture<BigDecimal>> futures = wealthComponents.stream()
                .map(component -> component.getValue(uuid))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Override
    public CompletableFuture<List<RankEntry>> getWealthRanking(List<String> targetComponents, int limit, int offset) {
        List<Integer> allTargetTypeCodes =  wealthComponents.stream()
                .filter(c -> targetComponents.contains(c.getName()))
                .flatMap(c -> c.getTargetTypes().stream())
                .map(AccountType::getCode)
                .distinct()
                .toList();

        if (allTargetTypeCodes.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return economyRepository.findWealthRanking(allTargetTypeCodes, limit, offset);
    }

    @Override
    public List<WealthComponent> getWealthComponent() {
        return wealthComponents;
    }

    @Override
    public List<String> getWealthComponentNames() {
        return wealthComponents.stream().map(WealthComponent::getName).toList();
    }

    @Override
    public void addWealthComponents(WealthComponent component) {
        wealthComponents.add(component);
    }
}
