package kr.shkworld.shktown.core.shop.model;

import java.math.BigDecimal;

public record ConfiguredShopItem(
        String id,
        int slot,
        ShopAction action,
        ShopCurrency currency,
        BigDecimal price,
        ShopItemSpec item
) {
}
