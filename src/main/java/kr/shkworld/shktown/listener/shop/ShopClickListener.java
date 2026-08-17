package kr.shkworld.shktown.listener.shop;

import kr.shkworld.shktown.ui.shop.ShopScreen;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class ShopClickListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ShopScreen screen && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < event.getInventory().getSize()) {
                screen.handleClick(player, slot, event.getClick());
            }
        }
    }
}
