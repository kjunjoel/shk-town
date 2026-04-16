package kr.shkworld.shktown.core.service;

import kr.shkworld.shktown.core.model.EconomyLog;
import kr.shkworld.shktown.core.model.LogType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface LogService {
    void log(LogType logType, String targetID, String message, Map<String, Object> data);
    void logEconomy(String targetID, String accountNumber, String action, BigDecimal amount, String reason);
    CompletableFuture<List<EconomyLog>> getRecentEconomyLogsAsync(String accountNumber, int limit);
}
