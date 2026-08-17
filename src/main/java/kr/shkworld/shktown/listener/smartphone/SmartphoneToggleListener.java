package kr.shkworld.shktown.listener.smartphone;

import kr.shkworld.shktown.ui.apps.SmartphoneManager;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class SmartphoneToggleListener implements Listener {
    private final SmartphoneManager smartphoneManager;

    public SmartphoneToggleListener(SmartphoneManager smartphoneManager) {
        this.smartphoneManager = smartphoneManager;
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        event.setCancelled(true);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.8f, 1.2f);
        smartphoneManager.openMainScreen(event.getPlayer());
    }
}
