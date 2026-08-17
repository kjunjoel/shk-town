package kr.shkworld.shktown.ui.apps;

import kr.shkworld.shktown.ui.SmartphoneScreen;
import kr.shkworld.shktown.util.GUIUtil;
import kr.shkworld.shktown.util.MessageUtil;
import kr.shkworld.shktown.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;


public class SmartphoneMainScreen implements SmartphoneScreen {
    private final Inventory inventory;
    private final Map<Integer, Consumer<Player>> appActions;

    public SmartphoneMainScreen(Player player, String title, ItemStack homeButton, Map<Integer, ItemStack> appItems, Map<Integer, Consumer<Player>> appActions) {
        this.appActions = appActions != null ? appActions : new HashMap<>();
        this.inventory = Bukkit.createInventory(this, 54, TextUtil.parse(title));

        initLayout(player, homeButton, appItems);
    }

    private void initLayout(Player player, ItemStack homeButton, Map<Integer, ItemStack> appItems) {
        ItemStack bezel = GUIUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, "&f", null);

        for (int i = 0; i < 54; i++) {
            if (i <= 3 || (i >= 5 && i <= 8) || i == 9 || i == 17 || i == 18 || i == 26 ||
                    i == 27 || i == 35 || i == 36 || i == 44 || i >= 45) {
                inventory.setItem(i, bezel);
            }
        }

        ItemStack profileCard = GUIUtil.createPlayerHead(
                player,
                "§6" + player.getName() + " §f님의 스마트폰",
                "§7--------------------",
                "§a 소속 직급 §f: 시민",
                "§6 보유 현금 §f: 0 원",
                "§b 보유 캐시 §f: 0",
                "§7--------------------"
        );
        inventory.setItem(4, profileCard);

        if (appItems != null) {
            appItems.forEach(inventory::setItem);
        }

        inventory.setItem(49, homeButton != null
                ? homeButton.clone()
                : GUIUtil.createItem(Material.COMPASS, "&cHOME", null));
    }

    @Override
    public void handleSlotClick(Player player, int slot) {
        if (appActions.containsKey(slot)) {
            appActions.get(slot).accept(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        ItemStack clickedItem = inventory.getItem(slot);
        if (clickedItem != null
                && clickedItem.getType() != Material.GRAY_STAINED_GLASS_PANE
                && clickedItem.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE
                && clickedItem.getType() != Material.PLAYER_HEAD) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            MessageUtil.send(player, "§c아직 구현 중인 기능입니다.");
        }
    }

    @Override
    public void handleSlotClick(Player player, int slot, ClickType clickType) {
        handleSlotClick(player, slot);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
