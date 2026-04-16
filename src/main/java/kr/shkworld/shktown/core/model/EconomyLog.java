package kr.shkworld.shktown.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record EconomyLog(
        long id,
        String targetID,
        String accountNumber,
        String actionType,
        BigDecimal amount,
        String reason,
        LocalDateTime createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("M/d HH:mm");

    public String getFormattedLog() {
        String color = actionType.equalsIgnoreCase("DEPOSIT") ? "§a+" : "§c-";
        return String.format("§7[%s] %s%,.0f원 §8(%s)",
                createdAt.format(FORMATTER),
                color,
                amount,
                reason);
    }
}
