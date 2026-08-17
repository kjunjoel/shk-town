package kr.shkworld.shktown.core.economy.service.impl;

import kr.shkworld.shktown.core.economy.model.AccountType;
import kr.shkworld.shktown.core.economy.model.TransactionReason;
import kr.shkworld.shktown.core.economy.repository.TransactionRepository;
import kr.shkworld.shktown.core.economy.service.AccountService;
import kr.shkworld.shktown.core.economy.service.EconomyService;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyServiceImpl implements EconomyService {
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public EconomyServiceImpl(
            AccountService accountService,
            TransactionRepository transactionRepository
    ) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public CompletableFuture<Boolean> transfer(
            UUID fromOwner, AccountType fromType,
            UUID toOwner, AccountType toType,
            BigDecimal amount, TransactionReason reason, String detail,
            String senderDisplay
    ) {
        return accountService.getAccountByOwnerAsync(fromOwner, fromType)
                .thenCombine(
                        accountService.getAccountByOwnerAsync(toOwner, toType),
                        (fromOpt, toOpt) -> {
                            if (fromOpt.isEmpty() || toOpt.isEmpty()) {
                                return null;
                            }
                            return new AccountNumbers(
                                    fromOpt.get().getAccountNumber(),
                                    toOpt.get().getAccountNumber()
                            );
                        }
                ).thenCompose(accountNumbers -> {
                    if (accountNumbers == null) {
                        return CompletableFuture.completedFuture(false);
                    }
                    return transferByAccountNumber(
                            accountNumbers.from(), accountNumbers.to(),
                            amount, reason, detail, senderDisplay
                    );
                });
    }

    @Override
    public CompletableFuture<Boolean> transferByAccountNumber(
            String fromAccountNumber, String toAccountNumber,
            BigDecimal amount, TransactionReason reason, String detail,
            String senderDisplay
    ) {
        if (fromAccountNumber == null || toAccountNumber == null || amount == null) {
            return CompletableFuture.completedFuture(false);
        }

        UUID transactionId = UUID.randomUUID();
        return transactionRepository.transfer(
                transactionId, fromAccountNumber, toAccountNumber,
                amount, reason, detail, senderDisplay
        ).thenCompose(result -> {
            if (!result.success()) {
                return CompletableFuture.completedFuture(false);
            }

            CompletableFuture<?> sourceRefresh = accountService.refreshAccountAsync(fromAccountNumber);
            CompletableFuture<?> targetRefresh = accountService.refreshAccountAsync(toAccountNumber);
            return CompletableFuture.allOf(sourceRefresh, targetRefresh).thenApply(ignored -> true);
        });
    }

    private record AccountNumbers(String from, String to) {
    }
}
