package kr.shkworld.shktown.core.economy.service;

import kr.shkworld.shktown.core.economy.model.AccountType;
import kr.shkworld.shktown.core.economy.model.TransactionReason;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {
    /**
     * 송금인으로부터 수취인에게 계좌이체합니다.
     * @param fromOwner 송금인
     * @param fromType 송금인의 계좌 유형
     * @param toOwner 수취인
     * @param toType 수취인의 계좌 유형
     * @param amount 금액
     * @param reason 계좌이체 사유
     * @param detail 세부사유
     * @return 계좌이체 성공 여부
     */
    CompletableFuture<Boolean> transfer(
            UUID fromOwner, AccountType fromType,
            UUID toOwner, AccountType toType,
            BigDecimal amount, TransactionReason reason, String detail,
            String senderDisplay
    );

    /**
     * 계좌번호를 이용해 계좌이체 합니다.
     * @param fromAccountNumber 송금인의 계좌번호
     * @param toAccountNumber 수취인의 계좌번호
     * @param amount 금액
     * @param reason 계좌이체 사유
     * @param detail 세부사유
     * @return 계좌이체 성공 여부
     */
    CompletableFuture<Boolean> transferByAccountNumber(
            String fromAccountNumber, String toAccountNumber,
            BigDecimal amount, TransactionReason reason, String detail,
            String senderDisplay
    );
}
