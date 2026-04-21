package kr.shkworld.shktown.core.model;

public enum LogType {
    ECONOMY("금전"),
    ADMIN("관리자"),
    ACCESS("접속"),
    ERROR("오류"),
    EVENT("이벤트"),
    ;

    private final String description;

    LogType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
