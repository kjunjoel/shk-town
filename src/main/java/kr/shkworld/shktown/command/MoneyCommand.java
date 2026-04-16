package kr.shkworld.shktown.command;

import kr.shkworld.shktown.core.model.*;
import kr.shkworld.shktown.core.service.EconomyService;
import kr.shkworld.shktown.core.service.LogService;
import kr.shkworld.shktown.core.service.UserService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MoneyCommand implements CommandExecutor, TabCompleter {
    private final EconomyService economyService;
    private final LogService logService;
    private final UserService userService;
    private final DecimalFormat df = new DecimalFormat("#,###");

    public MoneyCommand(EconomyService economyService, LogService logService, UserService userService) {
        this.economyService = economyService;
        this.logService = logService;
        this.userService = userService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 명령어는 게임 내에서만 사용할 수 있습니다");
            return true;
        }

        String suffix = label.equals("캐시") ? "캐시" : "원";
        if (args.length == 0) {
            handleSelfInfo(player, label, suffix);
            return true;
        }

        String subCommand = args[0];
        switch (subCommand) {
            case "순위" -> handleRanking(player, label, args, suffix);
            case "지급", "차감", "설정", "초기화" -> {
                if (player.isOp()) handleAdminCommand(player, label, args, suffix);
                else player.sendMessage("§c권한이 없습니다.");
            }
            default -> handleSelfInfo(player, label, suffix);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (!label.equals("캐시")) completions.add("순위");
            if (player.isOp() && !label.equals("재산")) {
                completions.add("지급");
                completions.add("차감");
            }
        }

        if (args.length == 2) {
            if (player.isOp() && !label.equals("재산")) {
                for (User user : userService.getOnlineUsers()) {
                    for (Account account : user.getAccountList()) {
                        completions.add(account.getAccountNumber());
                    }
                }
            }
        }

        return completions;
    }

    private void handleSelfInfo(Player player, String label, String suffix) {
        player.sendMessage("§7" + label + " 정보를 조회하고 있습니다...");
        userService.getUserAsync(player.getUniqueId()).thenAccept(userOpt -> {
            userOpt.ifPresentOrElse(user -> {
                List<Account> targets = switch (label) {
                    case "돈" -> user.getAccountsByType(AccountType.PERSONAL);
                    case "캐시" -> user.getAccountsByType(AccountType.CASH);
                    default -> user.getAccountList().stream()
                            .filter(acc -> acc.getAccountType() != AccountType.CASH).toList();
                };

                List<CompletableFuture<Component>> futureMessages = targets.stream()
                        .map(acc -> logService.getRecentEconomyLogsAsync(acc.getAccountNumber(), 10)
                                .thenApply(logs -> buildAccountComponent(acc, logs, suffix)))
                        .toList();

                CompletableFuture.allOf(futureMessages.toArray(new CompletableFuture[0])).thenAccept(v -> {
                    TextComponent.Builder message = Component.text();
                    message.append(Component.text("\n§e" + player.getName() + "§f님의 자산 정보\n"));
                    message.append(Component.text("§7--------------------------------\n"));

                    for (CompletableFuture<Component> future : futureMessages) {
                        message.append(future.join()).append(Component.text("\n"));
                    }

                    if (!label.equals("캐시")) {
                        BigDecimal total = targets.stream()
                                .map(Account::getBalance)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        message.append(Component.text("§7--------------------------------\n"));
                        message.append(Component.text("§f> §l총 자산 합계: §c" + df.format(total) + "원\n"));
                    }
                    message.append(Component.text("§7--------------------------------"));
                    player.sendMessage(message.build());
                });
            }, () -> {
                player.sendMessage("§c유저 정보를 불러오는 데 실패했습니다.");
            });
        });
    }

    private void handleRanking(Player player, String label, String[] args, String suffix) {
        int page = 1;
        if (args.length > 1) {
            try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }

        final int finalPage = page <= 0 ? 1 : page;
        player.sendMessage("§7" + label + " 순위를 불러오고 있습니다...");

        economyService.getRanking(label, finalPage).thenAccept(rankList -> {
            TextComponent.Builder message = Component.text();
            message.append(Component.text("\n§b[ SHK TOWN " + label + " 순위 (" + finalPage + "페이지) ]\n"));
            message.append(Component.text("§7------------------------------------\n"));

            if (rankList.isEmpty()) {
                message.append(Component.text("§7해당 페이지에 순위 정보가 없습니다."));
            } else {
                int startRank = (finalPage - 1) * 10 + 1;
                for (int i = 0; i < rankList.size(); i++) {
                    RankEntry entry = rankList.get(i);
                    int currentRank = startRank + i;
                    String color = (currentRank <= 3) ? "§6" : "§f";

                    message.append(Component.text(color + currentRank + "위 "))
                            .append(Component.text("§f" + entry.name() + " "))
                            .append(Component.text("§7| §a" + df.format(entry.amount()) + suffix + "\n"));
                }
            }

            message.append(Component.text("§7------------------------------------\n"));
            message.append(Component.text("§e[이전 페이지]").clickEvent(ClickEvent.runCommand("/" + label + " 순위 " + (finalPage - 1))))
                    .append(Component.text("§f | "))
                    .append(Component.text("§e[다음 페이지]").clickEvent(ClickEvent.runCommand("/" + label + " 순위 " + (finalPage + 1))));
            player.sendMessage(message.build());
        });
    }

    private void handleAdminCommand(Player admin, String label, String[] args, String suffix) {
        if (args.length < 3) {
            admin.sendMessage("§c사용법: /" + label + " <지급/차감> <계좌번호> <금액>");
            return;
        }

        String action = args[0];
        String accountNumber = args[1];
        BigDecimal amount;

        try {
            amount = new BigDecimal(args[2]);
            if (amount.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            admin.sendMessage("§c올바른 금액을 입력해주세요.");
            return;
        }

        userService.getUserByAccountNumber(accountNumber).thenAccept(userOpt -> {
           userOpt.ifPresentOrElse(user -> {
                String reason = "관리자 " + action + " (" + admin.getName() + ")";

                if (action.equals("지급")) {
                    economyService.deposit(user, accountNumber, amount, reason).thenRun(() -> {
                        admin.sendMessage("§f" + user.getName() + "§a님의 계좌(§f" + accountNumber + "§a)에 §e" + df.format(amount) + suffix + "§a을 지급했습니다.");
                    });
                } else if (action.equals("차감")) {
                    economyService.withdraw(user, accountNumber, amount, reason).thenAccept(success -> {
                       if (success) {
                           admin.sendMessage("§a[성공] §f" + user.getName() + "§a님의 계좌(§f" + accountNumber + "§a)에서 §e" + df.format(amount) + suffix + "§a을 차감했습니다.");
                       } else {
                           admin.sendMessage("§c[실패] §f" + user.getName() + "§c님의 계좌(§f" + accountNumber + "§c)에 잔액이 부족합니다.");
                       }
                    });
                } else {
                    admin.sendMessage("§c사용법: /" + label + " <지급/차감> <계좌번호> <금액>");
                }
           }, () -> admin.sendMessage("§c해당 계좌번호(§f" + accountNumber + "§c)를 가진 유저를 찾을 수 없습니다."));
        });
    }

    private Component buildAccountComponent(Account acc, List<EconomyLog> logs, String suffix) {
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
                .append(Component.text(df.format(acc.getBalance()) + suffix, NamedTextColor.GREEN))
                .build();
    }
}
