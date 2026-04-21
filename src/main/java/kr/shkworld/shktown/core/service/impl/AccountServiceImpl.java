package kr.shkworld.shktown.core.service.impl;

import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.TransactionReason;
import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.repository.AccountRepository;
import kr.shkworld.shktown.core.service.AccountService;
import kr.shkworld.shktown.core.service.UserService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AccountServiceImpl implements AccountService {
    private final UserService userService;
    private final AccountRepository accountRepository;

    public AccountServiceImpl(UserService userService, AccountRepository accountRepository) {
        this.userService = userService;
        this.accountRepository = accountRepository;
    }

    @Override
    public CompletableFuture<Account> createAccount(UUID owner, AccountType type) {
        return getAccountsByOwner(owner).thenCompose(accounts -> {
            int index = accounts.size() + 1;

            return CompletableFuture.supplyAsync(() -> {
                int typeCode = type.getCode();
                long timePart = (System.currentTimeMillis() / 1_000L) % 1_000_000L;
                int uuidPart = Math.abs(owner.hashCode() % 1_000);

                String accountNumber = String.format("%2d-%06d-%03d-%02d", typeCode, timePart, uuidPart, index);
                return new Account(owner, type, accountNumber, BigDecimal.ZERO);
            });
        }).thenCompose(newAccount -> saveAccount(newAccount).thenApply(v -> newAccount));
    }

    @Override
    public CompletableFuture<Boolean> deposit(String accountNumber, BigDecimal amount, TransactionReason reason, String detail) {
        return getAccount(accountNumber).thenCompose(accountOpt -> {
            if (accountOpt.isEmpty()) return CompletableFuture.completedFuture(false);

            Account account = accountOpt.get();
            account.deposit(amount);
            return saveAccount(account).thenApply(v -> true);
        });
    }

    @Override
    public CompletableFuture<Boolean> withdraw(String accountNumber, BigDecimal amount, TransactionReason reason, String detail) {
        return getAccount(accountNumber).thenCompose(accountOpt -> {
            if (accountOpt.isEmpty()) return CompletableFuture.completedFuture(false);

            Account account = accountOpt.get();
            if (!account.withdraw(amount)) {
                return CompletableFuture.completedFuture(false);
            }

            return saveAccount(account).thenApply(v -> true);
        });
    }

    @Override
    public CompletableFuture<Optional<Account>> getAccount(String accountNumber) {
        Optional<Account> cachedAccount = userService.getOnlineUsers()
                .values()
                .stream()
                .flatMap(user -> user.getAccounts().values().stream())
                .filter(acc -> acc.getAccountNumber().equals(accountNumber))
                .findFirst();

        if (cachedAccount.isPresent()) {
            return CompletableFuture.completedFuture(cachedAccount);
        }

        return accountRepository.findByNumber(accountNumber);
    }

    @Override
    public CompletableFuture<List<Account>> getAccountsByOwner(UUID owner) {
        User onlineUser = userService.getOnlineUsers().get(owner);

        if (onlineUser != null) {
            return CompletableFuture.completedFuture(new ArrayList<>(onlineUser.getAccounts().values()));
        }

        return accountRepository.findAllByOwner(owner);
    }

    @Override
    public CompletableFuture<Void> saveAccount(Account account) {
        try {
            return accountRepository.saveAccount(account);
        } catch (Exception ex) {
            throw new RuntimeException("계좌 저장 실패 (" + account.getAccountNumber() + ")", ex);
        }
    }
}
