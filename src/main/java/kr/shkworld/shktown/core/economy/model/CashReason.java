package kr.shkworld.shktown.core.economy.model;

public enum CashReason {
    DONATION("후원 보상"),
    SHOP_PURCHASE("상점 물품 구매"),
    ADMIN_ADJUST("관리자 조정"),
    EVENT_REWARD("이벤트 보상"),
    ;

    private final String description;

    CashReason(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
