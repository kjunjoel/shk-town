package kr.shkworld.shktown.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    @DisplayName("입금 성공")
    void deposit_success() {
        Account account = new Account(
                UUID.randomUUID(),
                AccountType.SAVINGS,
                "10-200000-300-40",
                new BigDecimal("1000")
        );

        account.deposit(new BigDecimal("500"));

        assertEquals(
                new BigDecimal("1500"),
                account.getBalance()
        );
    }

    @Test
    @DisplayName("0원 입금은 무시")
    void deposit_zero() {
        Account account = new Account(
                UUID.randomUUID(),
                AccountType.SAVINGS,
                "10-200000-300-40",
                new BigDecimal("1000")
        );

        account.deposit(BigDecimal.ZERO);

        assertEquals(
                new BigDecimal("1000"),
                account.getBalance()
        );
    }

    @Test
    @DisplayName("음수 입금은 무시")
    void deposit_negative() {
        Account account = new Account(
                UUID.randomUUID(),
                AccountType.SAVINGS,
                "10-200000-300-40",
                new BigDecimal("1000")
        );

        account.deposit(new BigDecimal("-500"));

        assertEquals(
                new BigDecimal("1000"),
                account.getBalance()
        );
    }

    @Test
    @DisplayName("출금 성공")
    void withdraw_success() {
        Account account = new Account(
                UUID.randomUUID(),
                AccountType.SAVINGS,
                "10-200000-300-40",
                new BigDecimal("1000")
        );

        boolean result =
                account.withdraw(new BigDecimal("300"));

        assertTrue(result);
        assertEquals(
                new BigDecimal("700"),
                account.getBalance()
        );
    }

    @Test
    @DisplayName("잔액 부족 출금 실패")
    void withdraw_insufficient() {
        Account account = new Account(
                UUID.randomUUID(),
                AccountType.SAVINGS,
                "10-200000-300-40",
                new BigDecimal("1000")
        );

        boolean result =
                account.withdraw(new BigDecimal("1500"));

        assertFalse(result);
        assertEquals(
                new BigDecimal("1000"),
                account.getBalance()
        );
    }

    @Test
    @DisplayName("0원 출금 실패")
    void withdraw_zero() {
        Account account = new Account(
                UUID.randomUUID(),
                AccountType.SAVINGS,
                "10-200000-300-40",
                new BigDecimal("1000")
        );

        boolean result =
                account.withdraw(BigDecimal.ZERO);

        assertFalse(result);
        assertEquals(
                new BigDecimal("1000"),
                account.getBalance()
        );
    }

    @Test
    @DisplayName("음수 출금 실패")
    void withdraw_negative() {
        Account account = new Account(
                UUID.randomUUID(),
                AccountType.SAVINGS,
                "10-200000-300-40",
                new BigDecimal("1000")
        );

        boolean result =
                account.withdraw(new BigDecimal("-100"));

        assertFalse(result);
        assertEquals(
                new BigDecimal("1000"),
                account.getBalance()
        );
    }
}