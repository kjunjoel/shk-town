package kr.shkworld.shktown.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyLogTest {
    private final LocalDateTime testTime = LocalDateTime.of(2026, 6, 8, 9, 30);

    @Nested
    @DisplayName("로그 메시지 포맷팅 테스트")
    class Formatting {

        @Test
        @DisplayName("예금 입금 로그: 초록색(+)과 '원' 단위가 정확해야 함")
        void format_SavingDeposit() {
            EconomyLog log = new EconomyLog(
                    1L, AccountType.SAVINGS, "user1", "123-456",
                    new BigDecimal("50000"), new BigDecimal("150000"),
                    "TRANSFER", "용돈", testTime
            );
            String result = log.getFormattedLog();
            assertTrue(result.contains("§a+50,000원"), "양수는 초록색(+)과 '원'이 붙어야 함");
            assertTrue(result.contains("(150,000원)"), "잔액 포맷팅 확인");
            assertTrue(result.contains("송금"), "TransactionReason 변환 확인");
            assertTrue(result.contains("6/8 09:30"), "날짜 포맷 확인");
        }

        @Test
        @DisplayName("캐시 사용 로그: 빨간색(-)과 '캐시' 단위가 정확해야 함")
        void format_CashWithdraw() {
            EconomyLog log = new EconomyLog(
                    2L, AccountType.CASH, "user1", null,
                    new BigDecimal("-1200"), new BigDecimal("8000"),
                    "SHOP_PURCHASE", "다이아몬드 구매", testTime
            );
            String result = log.getFormattedLog();
            assertTrue(result.contains("§c-1,200캐시"), "음수는 빨간색(-)과 '캐시'가 붙어야 함");
            assertTrue(result.contains("상점 물품 구매"), "CashReason 변환 확인");
        }
    }

    @Nested
    class DescriptionResolution {
        @Test
        @DisplayName("정의되지 않은 이유 코드가 들어와도 에러 없이 원본을 반환해야 함")
        void resolve_UnknownReason() {
            EconomyLog log = new EconomyLog(
                    3L, AccountType.SAVINGS, "user1", "123-456",
                    BigDecimal.TEN, BigDecimal.TEN,
                    "LUCKY_BOX", "상자깡", testTime
            );
            assertDoesNotThrow(log::getFormattedLog);
            assertTrue(log.getFormattedLog().contains("LUCKY_BOX"));
        }

        @Test
        @DisplayName("상세 내용(detail)이 null일 경우 빈 문자열로 처리되어야 함")
        void resolve_NullDetail() {
            EconomyLog log = new EconomyLog(
                    4L, AccountType.SAVINGS, "user1", "123-456",
                    BigDecimal.ONE, BigDecimal.ONE,
                    "ETC", null, testTime
            );
            String result = log.getFormattedLog();
            assertTrue(result.endsWith(": "), "detail이 null이면 빈 문자열이어야 함");
        }
    }
}
