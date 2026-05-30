package kr.shkworld.shktown.core.service;

import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.EconomyLog;
import kr.shkworld.shktown.core.model.LogType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface LogService {
    /**
     * 경제 로그를 기록합니다.
     * @param type 자산 종류
     * @param targetID 대상 유저의 UUID나 단체의 ID
     * @param accountNumber 계좌번호 (캐시의 경우 null)
     * @param amount 변동 금액
     * @param balanceAfter 변동 후 잔액
     * @param reason 변동 사유
     * @param detail 세부사유
     */
    CompletableFuture<Void> logEconomy(
            AccountType type, String targetID, String accountNumber,
            BigDecimal amount, BigDecimal balanceAfter, Enum<?> reason, String detail
    );

    /**
     * 시스템 로그를 기록합니다.
     * @param logType 로그 종류
     * @param message 로그 메시지
     * @param data 로그 데이터
     */
    CompletableFuture<Void> logSystem(LogType logType, String message, Map<String, Object> data);

    /**
     * 최근 몇 개의 경제 로그를 가져옵니다.
     * @param targetID 대상 유저의 UUID나 단체의 ID
     * @param type 자산 종류
     * @param limit 최대 개수
     * @return 경제 로그 리스트
     */
    CompletableFuture<List<EconomyLog>> getRecentLogsAsync(String targetID, AccountType type, int limit);
}
