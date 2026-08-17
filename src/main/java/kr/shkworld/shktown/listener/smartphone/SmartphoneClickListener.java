package kr.shkworld.shktown.listener.smartphone;

import kr.shkworld.shktown.ui.SmartphoneScreen;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class SmartphoneClickListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SmartphoneScreen screen && event.getWhoClicked() instanceof Player player) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < event.getInventory().getSize()) screen.handleSlotClick(player, slot, event.getClick());
        }
    }
}
