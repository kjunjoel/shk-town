package kr.shkworld.shktown.core.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 개별 계좌 정보를 관리하는 클래스입니다.
 * 예금, 투자 등 각 타입에 따른 잔액 관리 및 입출금 기능을 담당합니다.
 */
public class Account {
    private final UUID ownerUUID;
    private final AccountType accountType;  // 계좌의 유형
    private final String accountNumber;     // 계좌번호
    private BigDecimal balance;             // 현재 잔액

    public Account(UUID ownerUUID, AccountType accountType, String accountNumber, BigDecimal balance) {
        this.ownerUUID = ownerUUID;
        this.accountType = accountType;
        this.balance = balance;
        this.accountNumber = accountNumber;
    }

    /**
     * 계좌에 금액을 입금합니다.
     * @param amount 0보다 큰 금액
     */
    public void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }

    /**
     * 계좌에서 금액을 출금합니다.
     * @param amount 0보다 크고 잔액보다 작거나 같은 금액
     * @return 출금 성공 여부
     */
    public boolean withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (this.balance.compareTo(amount) < 0) return false;

        this.balance = this.balance.subtract(amount);
        return true;
    }

    // Getter
    public UUID getOwnerUUID() { return ownerUUID; }
    public AccountType getAccountType() { return accountType; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
}
