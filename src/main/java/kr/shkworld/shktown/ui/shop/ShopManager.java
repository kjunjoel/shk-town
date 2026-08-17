package kr.shkworld.shktown.ui.shop;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.core.shop.model.ConfiguredShop;
import kr.shkworld.shktown.core.shop.service.ShopService;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager {
    private final SHKTown plugin;
    private final ShopService shopService;
    private final Map<String, ConfiguredShop> shops = new ConcurrentHashMap<>();

    public ShopManager(SHKTown plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    public void clearShops() {
        shops.clear();
    }

    public void registerShop(ConfiguredShop shop) {
        shops.put(shop.id(), shop);
    }

    public Optional<ConfiguredShop> getShop(String shopId) {
        return Optional.ofNullable(shops.get(shopId));
    }

    public Collection<ConfiguredShop> getShops() {
        return shops.values();
    }

    public void openShop(Player player, String shopId) {
        getShop(shopId).ifPresent(shop -> player.openInventory(new ShopScreen(plugin, shop, shopService).getInventory()));
    }
}
