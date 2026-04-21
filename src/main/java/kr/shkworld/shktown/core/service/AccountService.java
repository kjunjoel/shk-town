package kr.shkworld.shktown.core.service;

import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.TransactionReason;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AccountService {
    /**
     * 신규 계좌를 만듭니다.
     * @param owner 유저 또는 마을, 국가, 기관의 UUID
     * @param type 계좌 유형
     * @return 비동기로 생성된 계좌
     */
    CompletableFuture<Account> createAccount(UUID owner, AccountType type);

    /**
     * 계좌에 일정 금액을 입금합니다.
     * @param accountNumber 계좌번호
     * @param amount 입금액
     * @param reason 입금 사유
     * @param detail 세부사유
     * @return 입금 성공 여부
     */
    CompletableFuture<Boolean> deposit(String accountNumber, BigDecimal amount, TransactionReason reason, String detail);

    /**
     * 계좌에서 일정 금액을 출금합니다.
     * @param accountNumber 계좌번호
     * @param amount 출금액
     * @param reason 출금 사유
     * @param detail 세부사유
     * @return 출금 성공 여부
     */
    CompletableFuture<Boolean> withdraw(String accountNumber, BigDecimal amount, TransactionReason reason, String detail);

    /**
     * 계좌번호로 계좌 객체를 가져옵니다.
     * @param accountNumber 계좌번호
     * @return 비동기로 가져온 Optional 계좌 객체
     */
    CompletableFuture<Optional<Account>> getAccount(String accountNumber);

    /**
     * 소유자의 UUID로 계좌 객체 목록을 가져옵니다.
     * @param owner 소유자의 UUID
     * @return 비동기로 가져온 계좌 객체 목록
     */
    CompletableFuture<List<Account>> getAccountsByOwner(UUID owner);

    /**
     * 계좌를 DB에 저장합니다.
     * @param account 계좌 객체
     */
    CompletableFuture<Void> saveAccount(Account account);
}
