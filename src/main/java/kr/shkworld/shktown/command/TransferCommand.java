package kr.shkworld.shktown.command;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.chat.EconomyFormatter;
import kr.shkworld.shktown.core.model.Account;
import kr.shkworld.shktown.core.model.AccountType;
import kr.shkworld.shktown.core.model.TransactionReason;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class TransferCommand implements CommandExecutor, TabCompleter {
    private final SHKTown plugin;
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^\\d{2}-\\d{6}-\\d{3}-\\d{2}$");

    private final Map<UUID, TransferRequest> pendingRequests = new HashMap<>();
    
    public TransferCommand(SHKTown plugin) { 
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        if (label.equals("송금확인")) {
            handleConfirm(player);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("§c사용법: /송금 <유저명/계좌번호> <금액>"));
            return true;
        }

        String targetInput = args[0];
        BigDecimal amount;
        try {
            amount = new BigDecimal(args[1]);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("§c올바른 금액을 입력해주세요."));
            return true;
        }

        AccountType fromType = (args.length >= 3 && args[2].equals("투자")) ? AccountType.INVESTMENT : AccountType.SAVINGS;

        plugin.getAccountService().getAccountsByOwner(player.getUniqueId()).thenAccept(acc -> {
            Optional<Account> fromAccOpt = acc.stream()
                    .filter(a -> a.getAccountType() == fromType)
                    .findFirst();
            
            if (fromAccOpt.isEmpty()) {
                player.sendMessage("§c본인의 " + fromType.getDescription() + " 계좌를 찾을 수 없습니다.");
                return;
            }

            resolveTargetAccount(targetInput).thenAccept(toAccNum -> {
                if (toAccNum == null) {
                    player.sendMessage("§c대상 계좌를 찾을 수 없습니다.");
                    return;
                }

                TransferRequest request = new TransferRequest(fromAccOpt.get().getAccountNumber(), toAccNum, amount);
                pendingRequests.put(player.getUniqueId(), request);
                sendConfirmMessage(player, request);
            });
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }

        return completions;
    }

    private CompletableFuture<String> resolveTargetAccount(String input) {
        if (ACCOUNT_PATTERN.matcher(input).matches()) {
            return CompletableFuture.completedFuture(input);
        }

        Player target = Bukkit.getPlayer(input);
        if (target == null) return CompletableFuture.completedFuture(null);

        return plugin.getAccountService().getAccountsByOwner(target.getUniqueId())
                .thenApply(accs -> accs.stream()
                        .filter(a -> a.getAccountType() == AccountType.SAVINGS)
                        .findFirst().map(Account::getAccountNumber).orElse(null));
    }

    private void sendConfirmMessage(Player player, TransferRequest req) {
        player.sendMessage(Component.text("\n§e[ 송금 승인 대기 ]")
                .append(Component.text("\n§f보내는 분: §7" + req.fromAcc()))
                .append(Component.text("\n§f받는 분: §b" + req.toAcc()))
                .append(Component.text("\n§f금액: §e" + EconomyFormatter.format(req.amount(), "원")))
                .append(Component.text("\n\n   §l[여기를 클릭하여 최종 승인]  ")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/송금확인")))
                .append(Component.text("\n§7(송금 버튼 클릭 시 즉시 처리되며 취소가 불가능합니다.)\n")));
    }

    private void handleConfirm(Player player) {
        TransferRequest req = pendingRequests.remove(player.getUniqueId());
        if (req == null) {
            player.sendMessage("§c진행 중인 송금 요청이 없거나 만료되었습니다.");
            return;
        }

        plugin.getEconomyService().transferByAccountNumber(
            req.fromAcc(), req.toAcc(), req.amount(),
            TransactionReason.TRANSFER, "계좌 송금"
        ).thenAccept(success -> {
            if (success) player.sendMessage("§a송금이 성공적으로 완료되었습니다!");
            else player.sendMessage("§c송금에 실패했습니다. 잔액 부족 혹은 계좌 상태를 확인하세요.");
        });
    }

    private record TransferRequest(String fromAcc, String toAcc, BigDecimal amount) {}
}