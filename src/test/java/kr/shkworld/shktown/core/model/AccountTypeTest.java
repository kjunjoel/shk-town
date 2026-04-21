package kr.shkworld.shktown.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTypeTest {
    @Nested
    @DisplayName("fromCode 메서드 테스트")
    class FromCode {

        @ParameterizedTest(name = "코드 {0} 입력 시 {1} 타입이 반환되어야 함")
        @DisplayName("정상적인 코드 입력 시 대응하는 Enum 상수를 반환한다")
        @CsvSource({
                "0, CASH",
                "10, SAVINGS",
                "11, INVESTMENT",
                "20, TOWN",
                "25, NATION",
                "30, INSTITUTION"
        })
        void fromCode_ShouldReturnCorrectType(int code, AccountType accountType) {
            assertEquals(accountType, AccountType.fromCode(code));
        }

        @ParameterizedTest(name = "잘못된 코드 {0} 입력 시 예외 발생")
        @DisplayName("정의되지 않은 코드를 입력하면 IllegalArgumentException이 발생한다")
        @ValueSource(ints = {-1, 5, 99, 1000})
        void fromCode_ShouldThrowExceptionForInvalidCode(int invalidCode) {
            Exception exception = assertThrows(IllegalArgumentException.class, () -> AccountType.fromCode(invalidCode));
            assertTrue(exception.getMessage().contains(String.valueOf(invalidCode)));
        }
    }

    @Nested
    @DisplayName("데이터 무결성 및 Getter 테스트")
    class DataIntegrity {

        @Test
        @DisplayName("모든 Enum 상수는 고유한 코드를 가져야 한다")
        void eachTypeShouldHaveUniqueCode() {
            AccountType[] types = AccountType.values();
            for (int i = 0; i < types.length; i++) {
                for (int j = i + 1; j < types.length; j++) {
                    assertNotEquals(types[i].getCode(), types[j].getCode(),
                            String.format("중복된 코드 발견: %s와 %s", types[i], types[j]));
                }
            }
        }

        @Test
        @DisplayName("설명(Description) 필드가 비어있지 않아야 한다")
        void descriptionShouldNotBeEmpty() {
            for (AccountType type : AccountType.values()) {
                assertNotNull(type.getDescription());
                assertFalse(type.getDescription().isEmpty(),
                        type.name() + "의 설명이 비어있습니다.");
            }
        }
    }
}
