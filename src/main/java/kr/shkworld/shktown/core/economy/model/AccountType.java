package kr.shkworld.shktown.core.economy.model;

public enum AccountType {
    SAVINGS(10, "예금"),
    INVESTMENT(11, "투자"),
    CITY(20, "마을"),
    STATE(25, "국가"),
    INSTITUTION(30, "기관"),
    ;

    private final int code;             // DB 저장용 정수 코드
    private final String description;   // 사용자 표시용 한글

    AccountType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * DB에서 읽어온 코드로 적절한 유형을 반환합니다.
     * @param code 정수형 타입 코드
     * @return 계좌 유형
     * @throws IllegalArgumentException 알 수 없는 코드가 들어올 경우 발생
     */
    public static AccountType fromCode(int code) {
        for (AccountType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("알 수 없는 계좌 타입 코드: " + code);
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
