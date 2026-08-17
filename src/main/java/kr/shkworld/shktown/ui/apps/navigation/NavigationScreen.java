package kr.shkworld.shktown.ui.apps.navigation;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.core.navigation.model.Position;
import kr.shkworld.shktown.core.navigation.service.NavigationService;
import kr.shkworld.shktown.ui.AbstractSmartphoneScreen;
import kr.shkworld.shktown.util.GUIUtil;
import kr.shkworld.shktown.util.LocationUtil;
import kr.shkworld.shktown.util.MessageUtil;
import kr.shkworld.shktown.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class NavigationScreen extends AbstractSmartphoneScreen {
    private final Player player;

    private int currentPage = 0;
    private NavigationSortType sortType = NavigationSortType.NAME_ASC;
    private String searchQuery = "";
    private final Map<Integer, String> destinationKeysBySlot = new java.util.HashMap<>();

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;

    public NavigationScreen(SHKTown plugin, Player player) {
        super(plugin, 54, plugin.getNavigationManager().getTitle());
        this.player = player;

        refresh();
    }

    private void refresh() {
        NavigationService navigationService = plugin.getNavigationService();
        NavigationManager navigationManager = plugin.getNavigationManager();
        ItemStack bezel = GUIUtil.createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&f", null);
        for (int slot = 0; slot < 54; slot++) {
            if (slot <= 3 || (slot >= 5 && slot <= 8) || slot == 9 || slot == 17 || slot == 18 || slot == 26
                    || slot == 27 || slot == 35 || slot == 36 || slot >= 45) {
                inventory.setItem(slot, bezel);
            }
        }
        applyCommonLayout();

        boolean isNavigating = navigationManager.isNavigating(player);
        String destinationName = navigationManager.getDestinationName(player);

        Map<String, Position> destinationMap = navigationService.getDestinations();
        Location playerLocation = player.getLocation();
        Position playerPosition = LocationUtil.toPosition(playerLocation);

        List<Map.Entry<String, Position>> filteredList = destinationMap.entrySet().stream()
                .filter(entry -> searchQuery.isEmpty() || navigationManager.getDestinationName(entry.getKey()).contains(searchQuery))
                .sorted((first, second) -> compareDestinations(first, second, playerPosition, navigationManager))
                .toList();

        int maxItemPerPage = CONTENT_SLOTS.length;
        int startIndex = currentPage * maxItemPerPage;
        int endIndex = Math.min(startIndex + maxItemPerPage, filteredList.size());
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredList.size() / maxItemPerPage));

        ItemStack status = GUIUtil.createItem(Material.PAPER, "&b&lGPS 상태", null,
                "&f내비게이션 상태: " + (isNavigating ? ("&a" + destinationName + "(으)로 안내 중") : "&c대기 중"),
                "&f페이지: &b" + (currentPage + 1) + "/" + totalPages,
                "&f현재 정렬 기준: &e" + sortType.getDisplayName(),
                "&f검색 필터: &b" + (searchQuery.isEmpty() ? "없음" : searchQuery)
        );
        inventory.setItem(4, status);

        destinationKeysBySlot.clear();
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Position> entry = filteredList.get(i);
            String destinationKey = entry.getKey();
            String destName = navigationManager.getDestinationName(destinationKey);
            Position destPos = entry.getValue();

            int slot = CONTENT_SLOTS[i - startIndex];

            boolean isSameWorld = playerLocation.getWorld() != null
                    && playerLocation.getWorld().getName().equalsIgnoreCase(destPos.world());

            String distanceText;
            if (isSameWorld) {
                double distance = playerPosition.distance(destPos);
                distanceText = String.format("&f거리: &b%.1fm", distance);
            } else {
                distanceText = "&f거리: &c다른 월드 (&e" + destPos.world() + "&c)";
            }

            ItemStack compass = navigationManager.getDestinationItem(destinationKey).clone();
            var meta = compass.getItemMeta();
            meta.lore(List.of(TextUtil.parse(String.format("&f좌표: &7X %.0f, Y: %.0f, Z: %.0f", destPos.x(), destPos.y(), destPos.z())),
                    TextUtil.parse(distanceText), TextUtil.parse("&f"), TextUtil.parse("&e클릭하여 길 안내 시작")));
            compass.setItemMeta(meta);

            inventory.setItem(slot, compass);
            destinationKeysBySlot.put(slot, destinationKey);
        }

        if (currentPage > 0) {
            ItemStack previousButton = plugin.getSmartphoneManager().getNavigationPreviousButton();
            inventory.setItem(PREVIOUS_PAGE_SLOT, previousButton != null ? previousButton : GUIUtil.createItem(Material.ARROW, "&a이전 페이지", null));
        }

        inventory.setItem(48, GUIUtil.createItem(Material.HOPPER,
                "&d정렬 방식 설정",
                null,
                "&f현재: &e" + sortType.getDisplayName(),
                "&f",
                "&e클릭 시 다음 정렬 방식으로 변경")
        );

        inventory.setItem(50, GUIUtil.createItem(Material.SPYGLASS,
                "&b목적지 검색",
                null,
                "&e좌클릭하여 검색어 입력",
                "&c우클릭하여 검색어 초기화")
        );

        inventory.setItem(52, GUIUtil.createItem(Material.BARRIER,
                "&c길 안내 중지",
                null,
                "&f현재 진행 중인 길 안내를 종료합니다.")
        );

        if (currentPage < totalPages - 1) {
            ItemStack nextButton = plugin.getSmartphoneManager().getNavigationNextButton();
            inventory.setItem(NEXT_PAGE_SLOT, nextButton != null ? nextButton : GUIUtil.createItem(Material.ARROW, "&a다음 페이지", null));
        }
    }

    @Override
    protected void handleAppClick(Player player, int slot, ClickType clickType) {
        NavigationService navigationService = plugin.getNavigationService();
        NavigationManager navigationManager = plugin.getNavigationManager();

        if (slot == PREVIOUS_PAGE_SLOT && currentPage > 0) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            currentPage--;
            refresh();
            return;
        }

        if (slot == 48) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            sortType = sortType.next();
            refresh();
            return;
        }

        if (slot == 50) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            MessageUtil.send(player, "&c아직 구현 중인 기능입니다.");
            return;
        }

        if (slot == 52) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            if (navigationManager.isNavigating(player)) {
                navigationManager.stopNavigation(player, false);
                refresh();
            } else {
                MessageUtil.send(player, "&c현재 진행 중인 길 안내가 없습니다.");
            }
            return;
        }

        if (slot == NEXT_PAGE_SLOT) {
            if (currentPage < Math.ceil((double) navigationService.getDestinations().size() / CONTENT_SLOTS.length) - 1) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                currentPage++;
                refresh();
            }
            return;
        }

        for (int contentSlot : CONTENT_SLOTS) {
            if (slot == contentSlot) {
                ItemStack item = inventory.getItem(slot);
                String destinationKey = destinationKeysBySlot.get(slot);
                if (item != null && destinationKey != null) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    Position destPos = navigationService.getDestination(destinationKey);
                    if (destPos != null) {
                        String destName = navigationManager.getDestinationName(destinationKey);
                        Location destLoc = LocationUtil.toLocation(destPos);

                        if (destLoc.getWorld() == null || !player.getWorld().equals(destLoc.getWorld())) {
                            MessageUtil.send(player, navigationManager.getDifferentWorld(), false);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1f, 2f);
                            return;
                        }

                        navigationManager.startNavigation(player, destName, destLoc);
                        player.closeInventory();
                    }
                }
                break;
            }
        }
    }

    private int compareDestinations(Map.Entry<String, Position> first, Map.Entry<String, Position> second,
                                    Position playerPosition, NavigationManager manager) {
        return switch (sortType) {
            case NAME_ASC -> manager.getDestinationName(first.getKey()).compareTo(manager.getDestinationName(second.getKey()));
            case NAME_DESC -> manager.getDestinationName(second.getKey()).compareTo(manager.getDestinationName(first.getKey()));
            case DISTANCE_ASC -> Double.compare(first.getValue().distanceSquared(playerPosition), second.getValue().distanceSquared(playerPosition));
            case DISTANCE_DESC -> Double.compare(second.getValue().distanceSquared(playerPosition), first.getValue().distanceSquared(playerPosition));
        };
    }

}
