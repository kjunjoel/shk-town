package kr.shkworld.shktown.core.repository;

import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.EconomyLog;
import kr.shkworld.shktown.core.model.LogType;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LogRepository {
    /**
     * 경제 로그를 저장합니다.
     * @param type 자산 유형
     * @param targetID 대상 ID
     * @param accountNumber 계좌번호
     * @param amount 금액
     * @param balanceAfter 변화 후 금액
     * @param reason 사유
     * @param detail 세부사유
     */
    CompletableFuture<Void> saveEconomyLog(
            AccountType type, String targetID, String accountNumber,
            BigDecimal amount, BigDecimal balanceAfter, Enum<?> reason, String detail
    );

    /**
     * 시스템 로그를 저장합니다.
     * @param type 로그 유형
     * @param message 메시지
     * @param dataJson JSON 형테의 데이터
     */
    CompletableFuture<Void> saveSystemLog(LogType type, String message, String dataJson);

    /**
     * 최근 몇 개의 경제 로그를 찾습니다.
     * @param targetID 대상 ID
     * @param limit 최대 개수
     * @return 경제 로그 목록
     */
    CompletableFuture<List<EconomyLog>> findRecentEconomyLogs(String targetID, AccountType type, int limit);
}
