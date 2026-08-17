package kr.shkworld.shktown.core.shop.service;

import kr.shkworld.shktown.core.shop.model.ConfiguredShopItem;
import kr.shkworld.shktown.core.shop.model.ShopResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ShopService {
    CompletableFuture<ShopResult> buy(UUID buyerUuid, ConfiguredShopItem shopItem);

    CompletableFuture<ShopResult> refundPurchase(UUID buyerUuid, ConfiguredShopItem shopItem);

    CompletableFuture<ShopResult> sell(UUID sellerUuid, ConfiguredShopItem shopItem);
}
