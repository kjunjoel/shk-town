package kr.shkworld.shktown.core.service.impl;

import kr.shkworld.shktown.core.model.EconomyLog;
import kr.shkworld.shktown.core.model.LogType;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.repository.LogRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class LogServiceImpl implements LogService {
    private final Logger logger;
    private final LogRepository logRepository;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    public LogServiceImpl(Logger logger, LogRepository logRepository) {
        this.logger = logger;
        this.logRepository = logRepository;
    }

    @Override
    public void log(LogType logType, String targetID, String message, Map<String, Object> data) {
        logger.info(String.format("[%s] %s: %s", logType, targetID, message));
        String dataJson = (data != null) ? gson.toJson(data) : "";
        logRepository.insertLog(logType.name(), targetID, message, dataJson);
    }

    @Override
    public void logEconomy(String targetID, String accountNumber, String action, BigDecimal amount, String reason) {
        logger.info(String.format("[ECONOMY %s -> %s: %f (%s)", targetID, action, amount, reason));
        logRepository.insertLogEconomy(targetID, accountNumber, action, amount, reason);
    }

    @Override
    public CompletableFuture<List<EconomyLog>> getRecentEconomyLogsAsync(String accountNumber, int limit) {
        return CompletableFuture.supplyAsync(() -> logRepository.findRecentEconomyLogs(accountNumber, limit));
    }
}
