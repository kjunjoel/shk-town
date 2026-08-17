package kr.shkworld.shktown.core.shop.model;

public enum ShopCurrency {
    WON,
    CASH;

    public static ShopCurrency fromString(String value) {
        if (value == null) {
            return WON;
        }
        try {
            return ShopCurrency.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WON;
        }
    }
}
