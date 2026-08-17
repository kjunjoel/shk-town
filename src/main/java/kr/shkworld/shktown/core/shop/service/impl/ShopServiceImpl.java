package kr.shkworld.shktown.core.shop.service.impl;

import kr.shkworld.shktown.core.economy.model.AccountType;
import kr.shkworld.shktown.core.economy.model.CashReason;
import kr.shkworld.shktown.core.economy.model.TransactionReason;
import kr.shkworld.shktown.core.economy.service.AccountService;
import kr.shkworld.shktown.core.economy.service.UserService;
import kr.shkworld.shktown.core.shop.model.ConfiguredShopItem;
import kr.shkworld.shktown.core.shop.model.ShopCurrency;
import kr.shkworld.shktown.core.shop.model.ShopResult;
import kr.shkworld.shktown.core.shop.service.ShopService;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ShopServiceImpl implements ShopService {
    private final AccountService accountService;
    private final UserService userService;

    public ShopServiceImpl(AccountService accountService, UserService userService) {
        this.accountService = accountService;
        this.userService = userService;
    }

    @Override
    public CompletableFuture<ShopResult> buy(UUID buyerUuid, ConfiguredShopItem shopItem) {
        if (buyerUuid == null || shopItem == null) {
            return CompletableFuture.completedFuture(ShopResult.failure("상품을 찾을 수 없습니다."));
        }
        if (shopItem.price().compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.completedFuture(ShopResult.failure("가격 설정이 올바르지 않습니다."));
        }

        CompletableFuture<Boolean> payment = switch (shopItem.currency()) {
            case WON -> accountService
                    .ensureAccountAsync(buyerUuid, AccountType.SAVINGS)
                    .thenCompose(account -> accountService.withdrawAsync(
                            account.getAccountNumber(),
                            shopItem.price(),
                            TransactionReason.SHOP_PURCHASE,
                            "system shop buy:" + shopItem.id()
                    ));
            case CASH -> userService.subtractCashAsync(
                    buyerUuid,
                    shopItem.price(),
                    CashReason.SHOP_PURCHASE,
                    "system shop buy:" + shopItem.id()
            );
        };

        return payment.thenCompose(paid -> {
            if (!paid) {
                return CompletableFuture.completedFuture(ShopResult.failure("잔액이 부족합니다."));
            }
            return CompletableFuture.completedFuture(ShopResult.success("구매가 완료되었습니다."));
        });
    }

    @Override
    public CompletableFuture<ShopResult> refundPurchase(UUID buyerUuid, ConfiguredShopItem shopItem) {
        if (buyerUuid == null || shopItem == null) {
            return CompletableFuture.completedFuture(ShopResult.failure("상품을 찾을 수 없습니다."));
        }
        if (shopItem.currency() == ShopCurrency.CASH) {
            return userService.addCashAsync(
                    buyerUuid,
                    shopItem.price(),
                    CashReason.ADMIN_ADJUST,
                    "system shop buy refund:" + shopItem.id()
            ).thenApply(ignored -> ShopResult.success("구매 금액을 환불했습니다."));
        }

        return accountService
                .ensureAccountAsync(buyerUuid, AccountType.SAVINGS)
                .thenCompose(account -> accountService.depositAsync(
                        account.getAccountNumber(),
                        shopItem.price(),
                        TransactionReason.ADMIN_ADJUST,
                        "system shop buy refund:" + shopItem.id()
                ))
                .thenApply(ignored -> ShopResult.success("구매 금액을 환불했습니다."));
    }

    @Override
    public CompletableFuture<ShopResult> sell(UUID sellerUuid, ConfiguredShopItem shopItem) {
        if (sellerUuid == null || shopItem == null) {
            return CompletableFuture.completedFuture(ShopResult.failure("상품을 찾을 수 없습니다."));
        }
        if (shopItem.currency() != ShopCurrency.WON) {
            return CompletableFuture.completedFuture(ShopResult.failure("기본 상점 판매 보상은 원화만 지원합니다."));
        }
        if (shopItem.price().compareTo(BigDecimal.ZERO) <= 0) {
            return CompletableFuture.completedFuture(ShopResult.failure("가격 설정이 올바르지 않습니다."));
        }

        return accountService
                .ensureAccountAsync(sellerUuid, AccountType.SAVINGS)
                .thenCompose(account -> accountService.depositAsync(
                        account.getAccountNumber(),
                        shopItem.price(),
                        TransactionReason.SHOP_SELL,
                        "system shop sell:" + shopItem.id()
                ))
                .handle((ignored, throwable) -> {
                    if (throwable == null) {
                        return ShopResult.success("판매가 완료되었습니다.");
                    }

                    return ShopResult.failure("판매 처리 중 오류가 발생했습니다.");
                });
    }
}
