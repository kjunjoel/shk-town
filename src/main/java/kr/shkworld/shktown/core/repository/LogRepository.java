package kr.shkworld.shktown.core.repository;

import kr.shkworld.shktown.core.model.EconomyLog;

import java.math.BigDecimal;
import java.util.List;

public interface LogRepository {
    void insertLog(String logType, String targetID, String message, String dataJson);
    void insertLogEconomy(String uuid, String accountNumber, String action, BigDecimal amount, String reason);
    List<EconomyLog> findRecentEconomyLogs(String accountNumber, int limit);
}
