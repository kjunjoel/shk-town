package kr.shkworld.shktown.core.shop.model;

import java.util.Map;
import java.util.Optional;

public record ConfiguredShop(
        String id,
        String title,
        int size,
        Map<Integer, ConfiguredShopItem> itemsBySlot
) {
    public Optional<ConfiguredShopItem> getItem(int slot) {
        return Optional.ofNullable(itemsBySlot.get(slot));
    }
}
