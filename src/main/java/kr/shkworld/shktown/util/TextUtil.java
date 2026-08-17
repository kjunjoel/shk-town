package kr.shkworld.shktown.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.regex.Pattern;


public final class TextUtil {
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("(?i)[&§]#([0-9a-f]{6})");
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("(?i)[&§]([0-9a-fk-or])");
    private static final Map<String, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry("0", "black"), Map.entry("1", "dark_blue"), Map.entry("2", "dark_green"), Map.entry("3", "dark_aqua"),
            Map.entry("4", "dark_red"), Map.entry("5", "dark_purple"), Map.entry("6", "gold"), Map.entry("7", "gray"),
            Map.entry("8", "dark_gray"), Map.entry("9", "blue"), Map.entry("a", "green"), Map.entry("b", "aqua"),
            Map.entry("c", "red"), Map.entry("d", "light_purple"), Map.entry("e", "yellow"), Map.entry("f", "white"),
            Map.entry("k", "obfuscated"), Map.entry("l", "bold"), Map.entry("m", "strikethrough"), Map.entry("n", "underlined"),
            Map.entry("o", "italic"), Map.entry("r", "reset")
    );
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI_MESSAGE.deserialize(toMiniMessage(text)).decoration(TextDecoration.ITALIC, false);
    }

    public static String plainText(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component).trim();
    }

    private static String toMiniMessage(String text) {
        String withHex = LEGACY_HEX_PATTERN.matcher(text).replaceAll("<#$1>");
        return LEGACY_CODE_PATTERN.matcher(withHex).replaceAll(match -> {
            String tag = LEGACY_TAGS.get(match.group(1).toLowerCase());
            return tag == null ? match.group() : "<" + tag + ">";
        });
    }

}
