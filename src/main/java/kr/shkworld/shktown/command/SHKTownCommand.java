package kr.shkworld.shktown.command;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SHKTownCommand implements CommandExecutor, TabCompleter {
    private final SHKTown plugin;

    public SHKTownCommand(SHKTown plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("shktown.admin")) {
                MessageUtil.sendNoPermission(sender);
                return true;
            }

            plugin.reload();
            MessageUtil.sendReloadSuccess(sender);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("shop")) {
            if (!sender.hasPermission("shktown.shop.open")) {
                MessageUtil.sendNoPermission(sender);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다."));
                return true;
            }

            String shopId = args.length >= 2 ? args[1] : "general";
            if (plugin.getShopManager().getShop(shopId).isEmpty()) {
                sender.sendMessage(Component.text("존재하지 않는 상점입니다: " + shopId));
                return true;
            }
            plugin.getShopManager().openShop(player, shopId);
            return true;
        }

        sender.sendMessage(Component.text(MessageUtil.getPrefix() + "/shktown reload - 설정 파일을 재불러옵니다."));
        sender.sendMessage(Component.text(MessageUtil.getPrefix() + "/shktown shop <id> - 시스템 상점을 엽니다."));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "shop");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("shop")) {
            return plugin.getShopManager().getShops().stream()
                    .map(shop -> shop.id())
                    .toList();
        }
        return Collections.emptyList();
    }
}
