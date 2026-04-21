package kr.shkworld.shktown.core.service;

import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.CashReason;
import kr.shkworld.shktown.core.model.RankEntry;
import kr.shkworld.shktown.core.model.TransactionReason;
import kr.shkworld.shktown.core.model.wealth.WealthComponent;

import java.math.BigDecimal;
import java.util.List;
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
            BigDecimal amount, TransactionReason reason, String detail
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
            BigDecimal amount, TransactionReason reason, String detail
    );

    /**
     * 특정 UUID의 예금 또는 투자, 마을, 국가 자산을 바꿉니다.
     * @param owner 유저 또는 마을, 국가의 UUID
     * @param type 계좌 종류
     * @param amount 바꿀 자산의 양
     * @param reason 캐시가 변경된 사유
     * @param detail 세부 사유
     * @return 자산을 성공적으로 바꿨는지 여부
     */
    CompletableFuture<Boolean> updateBalance(UUID owner, AccountType type, BigDecimal amount, TransactionReason reason, String detail);

    /**
     * 유저의 캐시를 바꿉니다.
     * @param uuid 유저의 UUID
     * @param amount 바꿀 캐시의 양
     * @param reason 캐시가 변경된 사유
     * @param detail 세부 사유
     * @return 캐시를 성공적으로 바꿨는지 여부
     */
    CompletableFuture<Boolean> updateCash(UUID uuid, BigDecimal amount, CashReason reason, String detail);

    /**
     * 전체 자산을 계산합니다.
     * @param uuid 유저의 UUID
     * @return 전체 자산액
     */
    CompletableFuture<BigDecimal> getTotalWealth(UUID uuid);

    /**
     * 자산 순위를 조회합니다.
     * @param targetComponents 대상 자산
     * @param limit 최대 조회 인원수
     * @param offset 몇 위부터 조회할지
     * @return 순위 객체 목록
     */
    CompletableFuture<List<RankEntry>> getWealthRanking(List<String> targetComponents, int limit, int offset);

    List<WealthComponent> getWealthComponent();
    List<String> getWealthComponentNames();
    void addWealthComponents(WealthComponent component);
}
