package kr.shkworld.shktown.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public interface SmartphoneScreen extends InventoryHolder {
    void handleSlotClick(Player player, int slot);

    void handleSlotClick(Player player, int slot, ClickType clickType);

    @Override
    @NotNull Inventory getInventory();
}
