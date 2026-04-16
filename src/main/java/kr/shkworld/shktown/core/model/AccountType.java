package kr.shkworld.shktown.core.model;

public enum AccountType {
    PERSONAL(10, "예금"),
    CASH(20, "캐시"),
    STOCK(30, "주식"),
    CRYPTO(40, "코인"),
    TOWN(50, "마을"),
    NATION(60, "국가"),
    ;

    private final int code;
    private final String description;

    AccountType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AccountType fromCode(int code) {
        for (AccountType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("알 수 없는 계좌 타입 코드: " + code);
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
