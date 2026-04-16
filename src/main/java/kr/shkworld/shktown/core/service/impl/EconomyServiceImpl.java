package kr.shkworld.shktown.core.service.impl;

import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.RankEntry;
import kr.shkworld.shktown.core.model.User;
import kr.shkworld.shktown.core.repository.UserRepository;
import kr.shkworld.shktown.core.service.EconomyService;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.service.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EconomyServiceImpl implements EconomyService {
    private final LogService logService;
    private final UserService userService;
    private final UserRepository userRepository;

    public EconomyServiceImpl(LogService logService, UserService userService, UserRepository userRepository) {
        this.logService = logService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    private Account getValidateAccount(User user, String accountNumber) {
        return user.getAccountOptional(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("계좌번호 " + accountNumber + "을(를) 찾을 수 없습니다."));
    }

    @Override
    public CompletableFuture<Void> deposit(User user, String accountNumber, BigDecimal amount, String reason) {
        return CompletableFuture.runAsync(() -> depositSync(user, accountNumber, amount, reason));
    }

    @Override
    public CompletableFuture<Boolean> withdraw(User user, String accountNumber, BigDecimal amount, String reason) {
        return CompletableFuture.supplyAsync(() -> withdrawSync(user, accountNumber, amount, reason));
    }

    @Override
    public CompletableFuture<Boolean> transfer(User from, User to, String fromAccountNumber, String toAccountNumber, BigDecimal amount, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            if (withdrawSync(from, fromAccountNumber, amount, "송금 출금: " + reason)) {
                depositSync(to, toAccountNumber, amount, "송금 입금: " + reason);
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<List<RankEntry>> getRanking(String label, int page) {
        return userRepository.getTopRanksAsync(label.equals("돈") ? AccountType.PERSONAL : null, page);
    }

    private void depositSync(User user, String accountNumber, BigDecimal amount, String reason) {
        Account account = getValidateAccount(user, accountNumber);
        account.deposit(amount);
        // account.setDirty(true);
        userService.saveUser(user);
        logService.logEconomy(user.getUUID().toString(), account.getAccountNumber(), "DEPOSIT", amount, reason);
    }

    private boolean withdrawSync(User user, String accountNumber, BigDecimal amount, String reason) {
        Account account = getValidateAccount(user, accountNumber);
        if (account.withdraw(amount)) {
            // account.setDirty(true);
            userService.saveUser(user);
            logService.logEconomy(user.getUUID().toString(), account.getAccountNumber(), "WITHDRAW", amount, reason);
            return true;
        }
        return false;
    }
}
