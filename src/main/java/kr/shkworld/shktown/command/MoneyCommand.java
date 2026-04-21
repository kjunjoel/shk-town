package kr.shkworld.shktown.command;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.core.formatter.EconomyFormatter;
import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.CashReason;
import kr.shkworld.shktown.core.model.TransactionReason;
import kr.shkworld.shktown.core.model.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MoneyCommand implements CommandExecutor, TabCompleter {
    private final SHKTown plugin;
    private final DecimalFormat df = new DecimalFormat("#,###");

    public MoneyCommand(SHKTown plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 명령어는 게임 내에서만 사용할 수 있습니다.");
            return true;
        }

        User user = plugin.getUserService().getUserFromCache(player.getUniqueId());
        if (user == null) {
            player.sendMessage("§c데이터를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
            return true;
        }

        String suffix = label.equals("캐시") ? "캐시" : "원";
        if (args.length == 0) {
            handleSelfInfo(player, label, suffix);
            return true;
        }

        String subCommand = args[0];
        switch (subCommand) {
            case "순위" -> handleRanking(player, label, args);
            case "지급", "차감", "설정" -> {
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
            return List.of();
        }
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (!label.equals("캐시")) completions.add("순위");
            if (player.isOp() && !label.equals("재산")) {
                completions.add("확인");
                completions.add("지급");
                completions.add("차감");
                completions.add("설정");
            }
        }

        if (args.length == 2) {
            if (player.isOp() && !label.equals("재산")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            }
        }

        return completions;
    }

    private void handleSelfInfo(Player player, String label, String suffix) {
        handleSelfInfo(player, player, label, suffix);
    }

    private void handleSelfInfo(Player player, Player showPlayer, String label, String suffix) {
        showPlayer.sendMessage("§7" + player.getName() + "님의 정보를 조회하고 있습니다...");
        User user = plugin.getUserService().getUserFromCache(player.getUniqueId());
        if (user == null) {
            showPlayer.sendMessage("§c유저 정보를 불러오는 데 실패했습니다.");
            return;
        }

        List<AccountType> targetTypes = switch (label) {
            case "캐시" -> List.of(AccountType.CASH);
            case "재산" -> plugin.getEconomyService().getWealthComponent().stream()
                    .flatMap(c -> c.getTargetTypes().stream())
                    .distinct()
                    .toList();
            default -> List.of(AccountType.SAVINGS, AccountType.INVESTMENT);
        };

        String uuid = player.getUniqueId().toString();
        List<CompletableFuture<Component>> futures = targetTypes.stream()
                .map(type -> user.getAccount(type)
                        .map(account -> plugin.getLogService().getRecentLogsAsync(uuid, type, 10)
                                .thenApply(logs -> EconomyFormatter.buildAccountComponent(account, logs, suffix)))
                        .orElseGet(() -> CompletableFuture.completedFuture(null)))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
            TextComponent.Builder message = Component.text()
                    .append(Component.text("\n§e" + player.getName() + "§f님의 자산 정보\n"))
                    .append(Component.text("§7--------------------------------\n"));

            futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .forEach(message::append);

            if (!label.equals("캐시")) {
                BigDecimal total = targetTypes.stream()
                        .map(user::getAccount)
                        .flatMap(Optional::stream)
                        .map(Account::getBalance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                message.append(Component.text("§7--------------------------------\n"));
                message.append(Component.text("§f> §l총 자산 합계: §c" + df.format(total) + "원\n"));
            }
            message.append(Component.text("§7--------------------------------"));
            TextComponent finalMessage = message.build();

            Bukkit.getScheduler().runTask(plugin, () -> {
                showPlayer.sendMessage(finalMessage);
            });
        }).exceptionally(ex -> {
            showPlayer.sendMessage("§c정보 조회 중 오류가 발생했습니다.");
            return null;
        });;
    }

    private void handleRanking(Player player, String label, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }

        final int finalPage = page <= 0 ? 1 : page;
        player.sendMessage("§7" + label + " 순위를 불러오고 있습니다...");

        List<String> targetComponents = switch (label) {
            case "빌딩" -> List.of("부동산");
            case "주식" -> List.of("주식자산");
            case "코인" -> List.of("가상화폐자산");
            case "재산" -> plugin.getEconomyService().getWealthComponentNames();
            default -> List.of("유동자산");
        };

        int limit = 10;
        int offset = (page - 1) * limit;
        plugin.getEconomyService().getWealthRanking(targetComponents, limit, offset).thenAccept(rankList -> {
                Component rankingMessage = EconomyFormatter.buildRankingMessage(label, rankList, finalPage);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(rankingMessage);
                });
        });
    }

    private void handleAdminCommand(Player admin, String label, String[] args, String suffix) {
        if (args.length < 2) {
            admin.sendMessage("§c사용법: /" + label + " <확인/지급/차감/설정> <닉네임> [금액]");
            return;
        }

        String action = args[0];
        String targetName = args[1];
        AccountType type = label.equals("캐시") ? AccountType.CASH : AccountType.SAVINGS;

        Player targetPlayer = Bukkit.getPlayer(targetName);
        User targetUser = (targetPlayer != null)
                ? plugin.getUserService().getUserFromCache(targetPlayer.getUniqueId())
                : null;

        if (targetUser == null) {
            admin.sendMessage("§c해당 유저를 찾을 수 없거나 현재 오프라인입니다.");
            return;
        }

        switch (action) {
            case "확인" -> handleSelfInfo(targetPlayer, admin, label, suffix);
            case "지급", "차감", "설정" -> {
                if (args.length < 3) {
                    admin.sendMessage("§c금액을 입력해주세요.");
                    return;
                }
                try {
                    BigDecimal amount = new BigDecimal(args[2]);
                    if (amount.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
                    modifyUserBalance(admin, targetUser, type, action, amount);
                } catch (NumberFormatException e) {
                    admin.sendMessage("§c올바른 금액 형식이 아닙니다.");
                }
            }
            default -> admin.sendMessage("§c알 수 없는 명령어입니다.");
        }
    }

    private void modifyUserBalance(Player admin, User targetUser, AccountType type, String action, BigDecimal amount) {
        plugin.getAccountService().getAccountsByOwner(targetUser.getUUID())
                .thenApply(accounts -> accounts.stream()
                        .filter(acc -> acc.getAccountType() == type)
                        .findFirst())
                .thenAccept(optAccount -> {
                    if (optAccount.isEmpty()) {
                        admin.sendMessage("§c해당 유저의 " + type.name() + " 계좌를 찾을 수 없습니다.");
                        return;
                    }

                    Account account = optAccount.get();
                    BigDecimal oldBalance = account.getBalance();

                    BigDecimal newBalance = switch (action) {
                        case "지급" -> oldBalance.add(amount);
                        case "차감" -> oldBalance.subtract(amount).max(BigDecimal.ZERO);
                        case "설정" -> amount;
                        default -> oldBalance;
                    };

                    String note = String.format("관리자(%s)에 의한 %s", admin.getName(), action);
                    CompletableFuture<Boolean> updateFuture;
                    if (type == AccountType.CASH) {
                        updateFuture = plugin.getEconomyService().updateCash(targetUser.getUUID(), newBalance, CashReason.ADMIN_ADJUST, note);
                    } else {
                        updateFuture = plugin.getEconomyService().updateBalance(targetUser.getUUID(), type, newBalance, TransactionReason.ADMIN_ADJUST, note);
                    }

                    updateFuture.thenAccept(isSuccess -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (isSuccess) {
                                admin.sendMessage(String.format("§a성공적으로 처리되었습니다. (§f%s §7-> §e%s§a)",
                                        df.format(oldBalance), df.format(newBalance)));

                                Player target = Bukkit.getPlayer(targetUser.getUUID());
                                if (target != null) {
                                    target.sendMessage("§e관리자에 의해 " + type.getDescription() + " 자산이 변경되었습니다.");
                                }
                            } else {
                                // 서비스 레이어에서 업데이트를 거부한 경우
                                admin.sendMessage("§c업데이트에 실패했습니다. (유효하지 않은 요청이거나 서비스 내부 오류)");
                            }
                        });
                    }).exceptionally(ex -> {
                        admin.sendMessage("§cDB 처리 중 오류가 발생했습니다.");
                        return null;
                    });
                }).exceptionally(ex -> {
                    admin.sendMessage("§c정보 조회 중 오류가 발생했습니다.");
                    return null;
                });
    }
}
