package kr.shkworld.shktown.core.economy.repository;

import kr.shkworld.shktown.core.economy.model.TransactionReason;
import kr.shkworld.shktown.core.economy.model.TransactionResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TransactionRepository {
    CompletableFuture<TransactionResult> transfer(
            UUID transactionId,
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            TransactionReason reason,
            String detail,
            String senderDisplay
    );

    CompletableFuture<TransactionResult> deposit(
            UUID transactionId,
            String accountNumber,
            BigDecimal amount,
            TransactionReason reason,
            String detail,
            String senderDisplay
    );

    CompletableFuture<TransactionResult> withdraw(
            UUID transactionId,
            String accountNumber,
            BigDecimal amount,
            TransactionReason reason,
            String detail,
            String senderDisplay
    );
}
