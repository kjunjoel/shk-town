package kr.shkworld.shktown.core.logging.repository;

import kr.shkworld.shktown.core.logging.model.LogType;

import java.util.concurrent.CompletableFuture;

public interface LogRepository {
    /**
     * 시스템 로그를 저장합니다.
     * @param type 로그 유형
     * @param message 메시지
     * @param dataJson JSON 형테의 데이터
     */
    CompletableFuture<Void> saveSystemLog(LogType type, String message, String dataJson);
}
