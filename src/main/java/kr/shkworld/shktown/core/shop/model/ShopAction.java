package kr.shkworld.shktown.core.shop.model;

public enum ShopAction {
    BUY,
    SELL;

    public static ShopAction fromString(String value) {
        if (value == null) {
            return BUY;
        }
        try {
            return ShopAction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BUY;
        }
    }
}
