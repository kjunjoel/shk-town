package kr.shkworld.shktown.core.logging.service;

import kr.shkworld.shktown.core.logging.model.LogType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface LogService {
    /**
     * 시스템 로그를 기록합니다.
     * @param logType 로그 종류
     * @param message 로그 메시지
     * @param data 로그 데이터
     */
    CompletableFuture<Void> logSystem(LogType logType, String message, Map<String, Object> data);
}
