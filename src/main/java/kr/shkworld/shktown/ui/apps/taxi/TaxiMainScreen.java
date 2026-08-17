package kr.shkworld.shktown.ui.apps.taxi;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.ui.AbstractSmartphoneScreen;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class TaxiMainScreen extends AbstractSmartphoneScreen {
    private final TaxiMapManager manager;
    private final Map<Integer, String> mapByButton;
    private final String defaultMap;
    private final String unavailableServiceMessage;

    public TaxiMainScreen(SHKTown plugin, TaxiMapManager manager, String title, Map<Integer, ItemStack> buttons,
                          Map<Integer, String> mapByButton, String defaultMap, String unavailableServiceMessage) {
        super(plugin, 54, title);
        this.manager = manager;
        this.mapByButton = Map.copyOf(mapByButton);
        this.defaultMap = defaultMap;
        this.unavailableServiceMessage = unavailableServiceMessage;
        this.buttons = Map.copyOf(buttons);
        initLayout();
    }

    private final Map<Integer, ItemStack> buttons;

    private void initLayout() {
        applyCommonLayout();
        buttons.forEach(inventory::setItem);
    }

    @Override
    protected void handleAppClick(Player player, int slot, ClickType clickType) {
        if (!buttons.containsKey(slot)) {
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        String mapId = mapByButton.get(slot);
        if (mapId != null) {
            if (!manager.hasTaxiCallPass(player)) {
                manager.sendNoCallPassMessage(player);
                return;
            }
            manager.openMap(player, mapId.isBlank() ? defaultMap : mapId);
        } else {
            kr.shkworld.shktown.util.MessageUtil.send(player, unavailableServiceMessage, false);
        }
    }
}
