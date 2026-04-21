package kr.shkworld.shktown.core.model;

import org.checkerframework.checker.units.qual.N;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {
    private User user;
    private final UUID testUUID =  UUID.randomUUID();
    private final String testName = "GyeongRye";

    @BeforeEach
    void setUp() {
        user = new User(testUUID, testName);
    }

    @Nested
    @DisplayName("계좌 관리 테스트")
    class AccountManagement {

        @Test
        @DisplayName("신규 계좌 추가 시 Optional로 정상 조회되어야 함")
        void addAndGetAccount_Success() {
            Account savings = new Account(testUUID, AccountType.SAVINGS, "123-456", new BigDecimal("50000"));
            user.addAccount(savings);
            assertTrue(user.getAccount(AccountType.SAVINGS).isPresent());
            assertEquals(savings, user.getAccount(AccountType.SAVINGS).get());
            assertEquals(new BigDecimal("50000"), user.getAccount(AccountType.SAVINGS).get().getBalance());
        }

        @Test
        @DisplayName("존재하지 않는 타입의 계좌 조회 시 빈 Optional 반환")
        void getAccount_Empty() {
            assertTrue(user.getAccount(AccountType.INVESTMENT).isEmpty());
        }

        @Test
        @DisplayName("이미 존재하는 타입의 계좌를 추가하면 갱신(Overwrite)되어야 함")
        void addAccount_Overwite() {
            Account oldAcc = new Account(testUUID, AccountType.SAVINGS, "OLD", BigDecimal.ZERO);
            Account newAcc = new Account(testUUID, AccountType.SAVINGS, "NEW", BigDecimal.ONE);
            user.addAccount(oldAcc);
            user.addAccount(newAcc);
            assertEquals("NEW", user.getAccount(AccountType.SAVINGS).get().getAccountNumber());
        }
    }

    @Nested
    @DisplayName("자산 및 소속 정보 테스트")
    class PropertyAndAffiliation {

        @Test
        @DisplayName("캐시 추가 시 누적 합산이 정확해야 함")
        void addCash_Accumulation() {
            user.addCash(new BigDecimal("1000.50"));
            user.addCash(new BigDecimal("2000.25"));
            assertEquals(0, new BigDecimal("3000.75").compareTo(user.getCash()));
        }

        @Test
        @DisplayName("마을 및 국가 ID 초기값은 -1(무소속)이어야 함")
        void defaultAffiliation_ShouldBeMinusOne() {
            assertEquals(-1L, user.getTownID());
            assertEquals(-1L, user.getNationID());
        }

        @Test
        @DisplayName("DB 로드용 생성자가 모든 필드를 정확히 초기화해야 함")
        void complexConstructor_Initialization() {
            Account acc1 = new Account(testUUID, AccountType.CASH, "C1", BigDecimal.TEN);
            List<Account> accountList = List.of(acc1);
            User loadedUser = new User(testUUID, "Bak", new BigDecimal("500"), accountList);
            assertEquals("Bak", loadedUser.getName());
            assertEquals(0, new BigDecimal("500").compareTo(loadedUser.getCash()));
            assertTrue(loadedUser.getAccount(AccountType.CASH).isPresent());
        }
    }
}
