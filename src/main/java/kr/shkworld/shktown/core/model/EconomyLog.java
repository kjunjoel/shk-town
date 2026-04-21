package kr.shkworld.shktown.core.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record EconomyLog(
        long id,
        AccountType assetType,
        String targetId,
        String accountNumber,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String reason,
        String detail,
        LocalDateTime createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("M/d HH:mm");

    public String getFormattedLog() {
        String color = amount.compareTo(BigDecimal.ZERO) > 0 ? "§a+" : "§c-";
        String unit = (assetType == AccountType.CASH) ? "캐시" : "원";
        String description = resolveDescription();

        return String.format("§7[%s] %s%,.0f%s§f (%,.0f%s) §8| %s: %s",
                createdAt.format(FORMATTER),
                color,
                amount,
                unit,
                balanceAfter,
                unit,
                description,
                detail == null ? "" : detail);
    }

    private String resolveDescription() {
        try {
            if (assetType == AccountType.CASH) {
                return CashReason.valueOf(reason).getDescription();
            } else {
                return TransactionReason.valueOf(reason).getDescription();
            }
        } catch (Exception e) {
            return reason;
        }
    }
}
