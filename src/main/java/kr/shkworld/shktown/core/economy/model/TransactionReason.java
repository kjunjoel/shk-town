package kr.shkworld.shktown.core.economy.model;

public enum TransactionReason {
    TRANSFER("송금"),
    SHOP_PURCHASE("상점 결제"),
    SHOP_SELL("상점 판매"),
    SALARY("급여"),
    INVESTMENT_BUY("주식/코인 매수"),
    INVESTMENT_SELL("주식/코인 매도"),
    TAX("세금 납부"),
    ADMIN_ADJUST("관리자 조정"),
    EVENT_REWARD("이벤트 보상"),
    ETC("기타"),
    ;

    private final String description;

    TransactionReason(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
