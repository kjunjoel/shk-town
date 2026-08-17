package kr.shkworld.shktown.ui.shop;

import kr.shkworld.shktown.SHKTown;
import kr.shkworld.shktown.core.shop.model.ConfiguredShop;
import kr.shkworld.shktown.core.shop.model.ConfiguredShopItem;
import kr.shkworld.shktown.core.shop.model.ShopAction;
import kr.shkworld.shktown.core.shop.model.ShopItemSpec;
import kr.shkworld.shktown.core.shop.service.ShopService;
import kr.shkworld.shktown.util.GUIUtil;
import kr.shkworld.shktown.util.MessageUtil;
import kr.shkworld.shktown.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public final class ShopScreen implements InventoryHolder {
    private final SHKTown plugin;
    private final ConfiguredShop shop;
    private final ShopService shopService;
    private final Inventory inventory;

    public ShopScreen(SHKTown plugin, ConfiguredShop shop, ShopService shopService) {
        this.plugin = plugin;
        this.shop = shop;
        this.shopService = shopService;
        this.inventory = Bukkit.createInventory(this, shop.size(), TextUtil.parse(shop.title()));
        render();
    }

    public void handleClick(Player player, int slot, ClickType clickType) {
        shop.getItem(slot).ifPresent(shopItem -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            if (shopItem.action() == ShopAction.SELL) {
                ItemStack item = toItemStack(shopItem.item());
                if (!removeItems(player, item)) {
                    MessageUtil.send(player, "판매할 아이템이 부족합니다.");
                    return;
                }

                shopService.sell(player.getUniqueId(), shopItem).thenAccept(result ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!result.success()) {
                                player.getInventory().addItem(item);
                            }
                            MessageUtil.send(player, result.message());
                        }));
                return;
            }

            shopService.buy(player.getUniqueId(), shopItem).thenAccept(result -> {
                if (!result.success()) {
                    Bukkit.getScheduler().runTask(plugin, () -> MessageUtil.send(player, result.message()));
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack item = toItemStack(shopItem.item());
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                    if (leftover.isEmpty()) {
                        MessageUtil.send(player, result.message());
                        return;
                    }

                    shopService.refundPurchase(player.getUniqueId(), shopItem)
                            .thenAccept(refund -> Bukkit.getScheduler().runTask(plugin,
                                    () -> MessageUtil.send(player, "인벤토리에 공간이 부족합니다.")));
                });
            });
        });
    }

    private void render() {
        shop.itemsBySlot().forEach((slot, shopItem) -> {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, displayItem(shopItem));
            }
        });
    }

    private ItemStack displayItem(ConfiguredShopItem shopItem) {
        ItemStack item = toItemStack(shopItem.item());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        ArrayList<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        if (meta.lore() != null) {
            lore.addAll(meta.lore());
        }
        lore.add(TextUtil.parse(""));
        lore.add(TextUtil.parse("&7구분: &f" + (shopItem.action() == ShopAction.BUY ? "구매" : "판매")));
        lore.add(TextUtil.parse("&7가격: &f" + shopItem.price().toPlainString() + " " + shopItem.currency().name()));
        lore.add(TextUtil.parse("&e클릭해서 진행합니다."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack toItemStack(ShopItemSpec spec) {
        Material material = Material.matchMaterial(spec.material());
        ItemStack item = GUIUtil.createItem(
                material != null ? material : Material.STONE,
                spec.name(),
                spec.model(),
                spec.lore().toArray(new String[0])
        );
        item.setAmount(spec.amount());
        return item;
    }

    private boolean removeItems(Player player, ItemStack required) {
        if (!player.getInventory().containsAtLeast(required, required.getAmount())) {
            return false;
        }
        player.getInventory().removeItem(required.clone());
        return true;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
