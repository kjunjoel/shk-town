package kr.shkworld.shktown.core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {
    private Account account;
    private final UUID testUUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        account = new Account(testUUID, AccountType.SAVINGS, "123-456", new BigDecimal("10000"));
    }

    @Test
    @DisplayName("입금 테스트: 정상적인 금액 입금 시 잔액이 증가해야 함")
    void deposit_Success() {
        account.deposit(new BigDecimal("5000"));
        assertEquals(0, new BigDecimal("15000").compareTo(account.getBalance()));
    }

    @ParameterizedTest
    @DisplayName("입금 실패 테스트: 0원 이하 입금 시 잔액 변화 없음")
    @ValueSource(strings = {"0", "-1000"})
    void deposit_Fail(String amountStr) {
        BigDecimal amount = new BigDecimal(amountStr);
        account.deposit(amount);
        assertEquals(0, new BigDecimal("10000").compareTo(account.getBalance()), "0원 이하 입금 시 잔액은 그대로여야 합니다.");
    }

    @Test
    @DisplayName("출금 테스트: 잔액 범위 내 출금 시 성공 및 잔액 감소")
    void withdraw_Success() {
        boolean result = account.withdraw(new BigDecimal("3000"));
        assertTrue(result);
        assertEquals(0, new BigDecimal("7000").compareTo(account.getBalance()));
    }

    @Test
    @DisplayName("출금 실패 테스트: 잔액보다 큰 금액 출금 시 실패")
    void withdraw_InsufficientBalance() {
        boolean result = account.withdraw(new BigDecimal("10001"));
        assertFalse(result, "잔액보다 큰 금액은 출금할 수 없습니다.");
        assertEquals(0, new BigDecimal("10000").compareTo(account.getBalance()), "실패 시 잔액이 변하면 안 됩니다.");
    }

    @ParameterizedTest
    @DisplayName("출금 실패 테스트: 0원 이하 출금 시도 시 실패")
    @ValueSource(strings = {"0", "-500"})
    void withdraw_InvalidAmount(String amountStr) {
        boolean result = account.withdraw(new BigDecimal(amountStr));
        assertFalse(result);
        assertEquals(0, new BigDecimal("10000").compareTo(account.getBalance()));
    }
}
