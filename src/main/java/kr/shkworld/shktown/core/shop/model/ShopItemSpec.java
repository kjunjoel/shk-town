package kr.shkworld.shktown.core.shop.model;

import java.util.List;

public record ShopItemSpec(
        String material,
        int amount,
        String name,
        String model,
        List<String> lore
) {
}
