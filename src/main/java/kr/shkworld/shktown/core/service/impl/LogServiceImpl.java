package kr.shkworld.shktown.core.service.impl;

import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.EconomyLog;
import kr.shkworld.shktown.core.model.LogType;
import kr.shkworld.shktown.core.repository.LogRepository;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.util.PluginLogger;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LogServiceImpl implements LogService {
    private final LogRepository logRepository;
    private final PluginLogger pluginLogger;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    public LogServiceImpl(LogRepository logRepository, PluginLogger pluginLogger) {
        this.logRepository = logRepository;
        this.pluginLogger = pluginLogger;
    }

    @Override
    public CompletableFuture<Void> logEconomy(AccountType type, String targetID, @Nullable String accountNumber, BigDecimal amount, BigDecimal balanceAfter, Enum<?> reason, String detail) {
        try {
            return logRepository.saveEconomyLog(type, targetID, accountNumber, amount, balanceAfter, reason, detail);
        } catch (Exception ex) {
            pluginLogger.severe(String.format("DB 로그 저장 실패: %s, %s, %s, %s%n", targetID, amount, balanceAfter, detail));
            throw new RuntimeException("DB 로그 저장 실패", ex);
        }
    }

    @Override
    public CompletableFuture<Void> logSystem(LogType logType, String message, Map<String, Object> data) {
        String dataJson = (data != null) ? gson.toJson(data) : "";
        return logRepository.saveSystemLog(logType, message, dataJson);
    }

    @Override
    public CompletableFuture<List<EconomyLog>> getRecentLogsAsync(String targetID, AccountType type, int limit) {
        return logRepository.findRecentEconomyLogs(targetID, type, limit);
    }
}
