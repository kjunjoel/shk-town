package kr.shkworld.shktown.ui;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.util.GUIUtil;
import kr.shkworld.shktown.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSmartphoneScreen implements SmartphoneScreen {
    protected final SHKTown plugin;
    protected final Inventory inventory;

    public AbstractSmartphoneScreen(SHKTown plugin, int size, String title) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, size, TextUtil.parse(title));
    }

    protected void applyCommonLayout() {
        ItemStack homeButton = plugin.getSmartphoneManager().getHomeButton();
        if (homeButton == null) {
            homeButton = GUIUtil.createItem(Material.COMPASS, "&cHOME", null);
        }
        inventory.setItem(49, homeButton);
    }

    protected boolean hasHomeButton() {
        return true;
    }

    @Override
    public void handleSlotClick(Player player, int slot) {
        if (hasHomeButton() && slot == 49) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            plugin.getSmartphoneManager().openMainScreen(player);
            return;
        }

        handleAppClick(player, slot, ClickType.LEFT);
    }

    @Override
    public void handleSlotClick(Player player, int slot, ClickType clickType) {
        if (hasHomeButton() && slot == 49) {
            handleSlotClick(player, slot);
            return;
        }

        handleAppClick(player, slot, clickType);
    }

    protected abstract void handleAppClick(Player player, int slot, ClickType clickType);

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
