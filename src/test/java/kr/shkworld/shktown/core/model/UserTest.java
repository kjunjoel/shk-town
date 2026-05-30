package kr.shkworld.shktown.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("기본 생성자 테스트")
    void constructor_default() {
        UUID uuid = UUID.randomUUID();

        User user = new User(uuid, "GyeongRye");

        assertEquals(uuid, user.getUUID());
        assertEquals("GyeongRye", user.getName());
        assertEquals(BigDecimal.ZERO, user.getCash());
        assertEquals(-1L, user.getTownID());
        assertEquals(-1L, user.getNationID());
        assertTrue(user.getAccounts().isEmpty());
    }

    @Test
    @DisplayName("DB 로드 생성자 테스트")
    void constructor_loadedAccounts() {
        UUID uuid = UUID.randomUUID();

        Account bankAccount = new Account(
                uuid,
                AccountType.SAVINGS,
                "10-200000-300-40",
                new BigDecimal("5000")
        );

        User user = new User(
                uuid,
                "Lantum",
                new BigDecimal("10000"),
                List.of(bankAccount)
        );

        assertEquals(new BigDecimal("10000"), user.getCash());

        assertTrue(user.getAccount(AccountType.SAVINGS).isPresent());
        assertEquals(bankAccount,
                user.getAccount(AccountType.SAVINGS).orElseThrow());
    }

    @Test
    @DisplayName("계좌 추가")
    void addAccount() {
        UUID uuid = UUID.randomUUID();

        User user = new User(uuid, "GyeongRye");

        Account account = new Account(
                uuid,
                AccountType.SAVINGS,
                "10-200000-300-40",
                BigDecimal.ZERO
        );

        user.addAccount(account);

        assertTrue(user.getAccount(AccountType.SAVINGS).isPresent());
        assertEquals(account,
                user.getAccount(AccountType.SAVINGS).orElseThrow());
    }

    @Test
    @DisplayName("없는 계좌 조회")
    void getAccount_notFound() {
        User user = new User(UUID.randomUUID(), "GyeongRye");

        assertTrue(user.getAccount(AccountType.SAVINGS).isEmpty());
    }

    @Test
    @DisplayName("캐시 추가")
    void addCash() {
        User user = new User(UUID.randomUUID(), "GyeongRye");

        user.addCash(new BigDecimal("1000"));
        user.addCash(new BigDecimal("500"));

        assertEquals(new BigDecimal("1500"), user.getCash());
    }

    @Test
    @DisplayName("같은 타입 계좌 추가 시 덮어쓰기")
    void addAccount_replace() {
        UUID uuid = UUID.randomUUID();

        User user = new User(uuid, "GyeongRye");

        Account oldAccount = new Account(
                uuid,
                AccountType.SAVINGS,
                "10-200000-300-40",
                BigDecimal.ZERO
        );

        Account newAccount = new Account(
                uuid,
                AccountType.SAVINGS,
                "10-500000-600-70",
                new BigDecimal("1000")
        );

        user.addAccount(oldAccount);
        user.addAccount(newAccount);

        Account result =
                user.getAccount(AccountType.SAVINGS).orElseThrow();

        assertEquals("222", result.getAccountNumber());
        assertEquals(new BigDecimal("1000"), result.getBalance());
    }

    @Test
    @DisplayName("Setter 테스트")
    void setters() {
        User user = new User(UUID.randomUUID(), "Steve");

        user.setName("Lantum");
        user.setTownID(10L);
        user.setNationID(20L);
        user.setCash(new BigDecimal("9999"));

        assertEquals("Lantum", user.getName());
        assertEquals(10L, user.getTownID());
        assertEquals(20L, user.getNationID());
        assertEquals(new BigDecimal("9999"), user.getCash());
    }
}