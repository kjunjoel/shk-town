package kr.shkworld.shktown.core.model;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 서버 내 사용자 정보를 담는 클래스입니다.
 * 사용자의 식별 정보(UUID, Name), 소속 정보(Town, Nation),
 * 그리고 보유 중인 다양한 유형의 계좌를 관리합니다.
 */
public class User {
    private final UUID uuid;        // 사용자의 UUID
    private String name;            // 사용자의 이름, 닉네임
    private long townID = -1L;      // 소속된 마을 ID (-1은 무소속)
    private long nationID = -1L;    // 소속된 국가 ID (-1은 무소속)

    // EnumMap 기반 계좌 목록
    private final Map<AccountType, Account> accounts = new EnumMap<>(AccountType.class);
    private BigDecimal cash = BigDecimal.ZERO; // 캐시 자산

    /**
     * 신규 사용자 생성을 위한 기본 생성자
     * @param uuid 사용자의 UUID
     * @param name 사용자의 이름, 닉네임
     */
    public User(UUID uuid, String name) {
        this(uuid, name, BigDecimal.ZERO, List.of());
    }

    /**
     * DB에서 데이터를 불러올 때 사용하는 기본 생성자
     * @param uuid 사용자의 UUID
     * @param name 사용자의 이름, 닉네임
     * @param cash 사용자가 가진 캐시
     * @param loadedAccounts 사용자의 계좌 목록
     */
    public User(UUID uuid, String name, BigDecimal cash, List<Account> loadedAccounts) {
        this.uuid = uuid;
        this.name = name;
        this.cash = cash;
        for (Account acc : loadedAccounts) {
            this.accounts.put(acc.getAccountType(), acc);
        }
    }

    /**
     * 사용자의 계좌 목록에 새로운 계좌를 추가하거나 갱신합니다.
     * @param account 계좌 객체
     */
    public void addAccount(Account account) {
        this.accounts.put(account.getAccountType(), account);
    }

    /**
     * 특정 타입의 계좌를 가져옵니다.
     * @param type 계좌 유형
     * @return 계좌가 존재하지 않을 수 있으므로 Optional로 반환합니다.
     */
    public Optional<Account> getAccount(AccountType type) {
        return Optional.ofNullable(accounts.get(type));
    }

    /**
     * 사용자의 캐시 잔액을 추가합니다.
     * @param amount 추가할 캐시 양
     */
    public void addCash(BigDecimal amount) {
        this.cash = this.cash.add(amount);
    }

    // Getter와 Setter
    public UUID getUUID() { return uuid; }
    public String getName() { return name; }
    public long getTownID() { return townID; }
    public long getNationID() { return nationID; }
    public BigDecimal getCash() { return cash; }
    public Map<AccountType, Account> getAccounts() { return accounts; }

    public void setName(String name) { this.name = name; }
    public void setTownID(long id) { this.townID = id; }
    public void setNationID(long id) { this.nationID = id; }
    public void setCash(BigDecimal cash) { this.cash = cash; }
}
