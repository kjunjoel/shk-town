package kr.shkworld.shktown.chat;

import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.EconomyLog;
import kr.shkworld.shktown.core.model.RankEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public class EconomyFormatter {
    private static final DecimalFormat DF = new DecimalFormat("#,###");

    public static String format(BigDecimal amount, String suffix) {
        return DF.format(amount) + suffix;
    }

    public static Component buildAccountComponent(Account acc, List<EconomyLog> logs, String suffix) {
        String accNum = acc.getAccountNumber();

        TextComponent.Builder hoverText = Component.text();
        hoverText.append(Component.text("§6§l[ 최근 거래 내역 ]\n"));

        if (logs.isEmpty()) {
            hoverText.append(Component.text("§7최근 거래 기록이 없습니다.\n"));
        } else {
            for (EconomyLog log : logs) {
                hoverText.append(Component.text(log.getFormattedLog() + "\n"));
            }
        }
        hoverText.append(Component.text("§e클릭하여 계좌번호를 복사합니다."));

        return Component.text()
                .append(Component.text("§f> §7[" + acc.getAccountType().getDescription() + "] "))
                .append(Component.text(accNum)
                        .color(NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.copyToClipboard(accNum))
                        .hoverEvent(HoverEvent.showText(hoverText.build())))
                .append(Component.text(" §8| §f잔액: "))
                .append(Component.text(DF.format(acc.getBalance()) + suffix, NamedTextColor.GREEN))
                .build();
    }

    public static Component buildRankingMessage(String title, List<RankEntry> ranking, int page) {
        TextComponent.Builder message = Component.text()
                .append(Component.text("§8--------------------------------\n"))
                .append(Component.text("§6§l[ " + title + " 순위 (" + page + "페이지) ]\n\n"));

        if (ranking.isEmpty()) {
            message.append(Component.text("§7해당 페이지에 순위 정보가 없습니다."));
        } else {
            for (int i = 0; i < ranking.size(); i++) {
                RankEntry entry = ranking.get(i);
                int rank = ((page - 1) * 10) + i + 1;

                String color = switch (rank) {
                    case 1 -> "§e§l"; // 금
                    case 2 -> "§f§l"; // 은
                    case 3 -> "§6§l"; // 동
                    default -> "§7";
                };

                message.append(Component.text(String.format("%s%d. §f%s §8: §a%s원\n",
                        color, rank, entry.name(), format(entry.amount(), "원"))));
            }
        }

        return message.append(Component.text("\n§8--------------------------------")).build();
    }
}
