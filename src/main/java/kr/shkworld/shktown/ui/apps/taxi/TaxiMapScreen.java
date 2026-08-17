package kr.shkworld.shktown.ui.apps.taxi;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.core.navigation.model.Position;
import kr.shkworld.shktown.ui.AbstractSmartphoneScreen;
import kr.shkworld.shktown.util.LocationUtil;
import kr.shkworld.shktown.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class TaxiMapScreen extends AbstractSmartphoneScreen {
    private final TaxiMapManager manager;
    private final Map<String, String> navigation;
    private final Map<Integer, ItemStack> stopItems;
    private final Map<Integer, Position> stopPositions;
    private final Map<Integer, String> stopNames;
    private final boolean showHomeButton;

    private static final int PREVIOUS_MAP_SLOT = 45;
    private static final int NEXT_MAP_SLOT = 53;

    public TaxiMapScreen(SHKTown plugin, TaxiMapManager manager, String title, Map<String, String> navigation,
                         Map<Integer, ItemStack> stopItems, Map<Integer, Position> stopPositions, Map<Integer, String> stopNames,
                         boolean showHomeButton) {
        super(plugin, 54, title);
        this.manager = manager;
        this.navigation = Map.copyOf(navigation);
        this.stopItems = Map.copyOf(stopItems);
        this.stopPositions = Map.copyOf(stopPositions);
        this.stopNames = Map.copyOf(stopNames);
        this.showHomeButton = showHomeButton;

        initLayout();
    }

    private void initLayout() {
        if (showHomeButton) {
            applyCommonLayout();
        }

        if (navigation.containsKey("previous")) {
            inventory.setItem(PREVIOUS_MAP_SLOT, plugin.getSmartphoneManager().getNavigationPreviousButton());
        }
        if (navigation.containsKey("next")) {
            inventory.setItem(NEXT_MAP_SLOT, plugin.getSmartphoneManager().getNavigationNextButton());
        }

        stopItems.forEach(inventory::setItem);
    }

    @Override
    protected boolean hasHomeButton() {
        return showHomeButton;
    }

    @Override
    protected void handleAppClick(Player player, int slot, ClickType clickType) {
        String direction = slot == PREVIOUS_MAP_SLOT ? "previous" : slot == NEXT_MAP_SLOT ? "next" : null;
        if (direction != null && navigation.get(direction) instanceof String nextMap) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            manager.openMap(player, nextMap, showHomeButton);
            return;
        }

        Position stopPosition = stopPositions.get(slot);
        if (stopPosition != null) {
            String stopName = stopNames.get(slot);
            player.closeInventory();

            String mainTitleStr = manager.getTitleMain().replace("{stop_name}", stopName);
            String subTitleStr = manager.getTitleSub().replace("{stop_name}", stopName);
            MessageUtil.sendTitle(player, mainTitleStr, subTitleStr,
                                  manager.getTitleFadeInMs(), manager.getTitleStayMs(), manager.getTitleFadeOutMs());
            player.playSound(player.getLocation(), Sound.ENTITY_MINECART_RIDING, 1.0f, 1.0f / ((float) manager.getTeleportDelayTicks() / 40));

            Location targetLocation = LocationUtil.toLocation(stopPosition);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && targetLocation != null) {
                    player.teleport(targetLocation);
                    player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 1.0f);

                    String arrivedMessage = manager.getArrivedMessage().replace("{stop_name}", stopName);
                    MessageUtil.send(player, arrivedMessage, manager.isUseGlobalPrefix());
                }
            }, manager.getTeleportDelayTicks());
        }
    }

}
