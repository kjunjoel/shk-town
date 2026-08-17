package kr.shkworld.shktown.listener.taxi;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.util.TextUtil;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class TaxiNpcInteractionListener implements Listener {
    private final SHKTown plugin;

    public TaxiNpcInteractionListener(SHKTown plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!isConfiguredTaxiNpc(entity)) return;

        event.setCancelled(true);
        plugin.getTaxiMapManager().openNpcMap(event.getPlayer(), plugin.getTaxiMapManager().getDefaultMap());
    }

    private boolean isConfiguredTaxiNpc(Entity entity) {
        var taxiManager = plugin.getTaxiMapManager();
        if (!taxiManager.isNpcEnabled()) return false;
        if (taxiManager.getNpcName().isBlank()) return true;
        String name = TextUtil.plainText(entity.customName());
        return taxiManager.getNpcName().equals(name);
    }
}
