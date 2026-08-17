package kr.shkworld.shktown.core.shop.model;

public record ShopResult(boolean success, String message) {
    public static ShopResult success(String message) {
        return new ShopResult(true, message);
    }

    public static ShopResult failure(String message) {
        return new ShopResult(false, message);
    }
}
