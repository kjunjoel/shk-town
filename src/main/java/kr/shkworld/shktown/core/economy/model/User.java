package kr.shkworld.shktown.core.economy.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class User {
    private final UUID uuid; // 유저의 UUID
    private String name; // 유저의 이름, 닉네임
    private final BigDecimal cash; // 캐시 자산
    private LocalDateTime lastLogin; // 마지막 접속 일자 및 시간
    private final LocalDateTime firstLogin; // 최초 접속 일자 및 시간

    /**
     * 신규 유저 생성을 위한 기본 생성자
     * @param uuid 유저의 UUID
     * @param name 유저의 이름, 닉네임
     */
    public User(UUID uuid, String name) {
        this(uuid, name, BigDecimal.ZERO, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * DB에서 데이터를 불러올 때 사용하는 기본 생성자
     * @param uuid 유저의 UUID
     * @param name 유저의 이름, 닉네임
     * @param cash 유저가 가진 캐시
     * @param lastLogin 유저의 마지막 접속 일자 및 시간
     * @param firstLogin 유저의 최초 접속 일자 및 시간
     */
    public User(UUID uuid, String name, BigDecimal cash,
                LocalDateTime lastLogin, LocalDateTime firstLogin) {
        this.uuid = uuid;
        this.name = name;
        this.cash = (cash == null) ? BigDecimal.ZERO : cash;
        this.lastLogin = lastLogin;
        this.firstLogin = firstLogin;
    }

    public UUID getUuid() { return uuid; }
    public synchronized String getName() { return name; }
    public BigDecimal getCash() { return cash; }
    public synchronized LocalDateTime getLastLogin() { return lastLogin; }
    public LocalDateTime getFirstLogin() { return firstLogin; }

    public synchronized void setName(String name) { this.name = name; }
    public synchronized void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
}
