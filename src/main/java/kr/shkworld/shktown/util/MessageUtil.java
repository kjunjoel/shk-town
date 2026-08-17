package kr.shkworld.shktown.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

public class MessageUtil {
    private static String prefix = "";
    private static String reloadSuccess = "";
    private static String noPermission = "";

    private MessageUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    public static void initGlobalConfig(String prefix, String reloadSuccess, String noPermission) {
        MessageUtil.prefix = prefix;
        MessageUtil.reloadSuccess = reloadSuccess;
        MessageUtil.noPermission = noPermission;
    }

    public static void send(CommandSender sender, String message) {
        send(sender, message, true);
    }

    public static void send(CommandSender sender, String message, boolean withPrefix) {
        if (sender != null && message != null) {
            String finalMessage = withPrefix ? prefix + message : message;
            finalMessage = finalMessage.replace("&", "§");
            sender.sendMessage(TextUtil.parse(finalMessage));
        }
    }

    public static void sendTitle(Player player, String mainTitle, String subTitle) {
        sendTitle(player, mainTitle, subTitle, 250, 1500, 250);
    }

    public static void sendTitle(Player player, String mainTitle, String subTitle,
                                 long fadeInMs, long stayMs, long fadeOutMs) {
        if (player == null) return;

        Component main = TextUtil.parse(mainTitle);
        Component sub = TextUtil.parse(subTitle);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeInMs),
                Duration.ofMillis(stayMs),
                Duration.ofMillis(fadeOutMs)
        );

        Title titleObject = Title.title(main, sub, times);
        player.showTitle(titleObject);
    }

    public static void sendReloadSuccess(CommandSender sender) {
        send(sender, reloadSuccess);
    }

    public static void sendNoPermission(CommandSender sender) {
        send(sender, noPermission);
    }

    public static String getPrefix() {
        return prefix;
    }
}
