package kr.shkworld.shktown.core.service;

import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.RankEntry;
import kr.shkworld.shktown.core.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {
    CompletableFuture<Void> deposit(User user, String accountNumber, BigDecimal amount, String reason);
    CompletableFuture<Boolean> withdraw(User user, String accountNumber, BigDecimal amount, String reason);
    CompletableFuture<Boolean> transfer(User from, User to, String fromAccountNumber, String toAccountNumber, BigDecimal amount, String reason);
    CompletableFuture<List<RankEntry>> getRanking(String label, int page);
}
