package com.example.common.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AbstractEnumSetJsonConverter 테스트")
class AbstractEnumSetJsonConverterTest {

    /** 테스트용 Enum */
    enum TestStatus {
        ACTIVE, INACTIVE, PENDING;

        public static TestStatus fromString(String code) {
            if (code == null || code.isBlank()) {
                return null;
            }
            try {
                return valueOf(code.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** 테스트용 컨버터 구현 */
    static class TestStatusSetConverter extends AbstractEnumSetJsonConverter<TestStatus> {
        public TestStatusSetConverter() {
            super(TestStatus.class, TestStatus::fromString);
        }
    }

    private TestStatusSetConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TestStatusSetConverter();
    }

    @Nested
    @DisplayName("convertToDatabaseColumn 테스트")
    class ToDatabaseTest {

        @Test
        @DisplayName("null을 빈 JSON 배열로 변환한다")
        void convertsNullToEmptyArray() {
            String result = converter.convertToDatabaseColumn(null);
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("빈 Set을 빈 JSON 배열로 변환한다")
        void convertsEmptySetToEmptyArray() {
            String result = converter.convertToDatabaseColumn(Collections.emptySet());
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("단일 값을 JSON 배열로 변환한다")
        void convertsSingleValueToJsonArray() {
            Set<TestStatus> values = EnumSet.of(TestStatus.ACTIVE);
            String result = converter.convertToDatabaseColumn(values);
            assertThat(result).isEqualTo("[\"ACTIVE\"]");
        }

        @Test
        @DisplayName("복수 값을 JSON 배열로 변환한다")
        void convertsMultipleValuesToJsonArray() {
            Set<TestStatus> values = EnumSet.of(
                    TestStatus.ACTIVE,
                    TestStatus.INACTIVE,
                    TestStatus.PENDING
            );
            String result = converter.convertToDatabaseColumn(values);

            assertThat(result)
                    .contains("\"ACTIVE\"")
                    .contains("\"INACTIVE\"")
                    .contains("\"PENDING\"")
                    .startsWith("[")
                    .endsWith("]");
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute 테스트")
    class ToEntityTest {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"[]", "  []  ", " "})
        @DisplayName("null, 빈 문자열, 빈 배열은 빈 Set을 반환한다")
        void returnsEmptySetForNullOrEmptyOrBlank(String dbData) {
            Set<TestStatus> result = converter.convertToEntityAttribute(dbData);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("단일 값 JSON을 Set으로 변환한다")
        void convertsSingleValueJson() {
            Set<TestStatus> result = converter.convertToEntityAttribute("[\"ACTIVE\"]");
            assertThat(result).containsExactly(TestStatus.ACTIVE);
        }

        @Test
        @DisplayName("복수 값 JSON을 Set으로 변환한다")
        void convertsMultipleValuesJson() {
            Set<TestStatus> result = converter.convertToEntityAttribute(
                    "[\"ACTIVE\",\"INACTIVE\",\"PENDING\"]"
            );
            assertThat(result).containsExactlyInAnyOrder(
                    TestStatus.ACTIVE,
                    TestStatus.INACTIVE,
                    TestStatus.PENDING
            );
        }

        @Test
        @DisplayName("소문자 값도 변환된다")
        void convertsLowerCaseValues() {
            Set<TestStatus> result = converter.convertToEntityAttribute("[\"active\",\"inactive\"]");
            assertThat(result).containsExactlyInAnyOrder(
                    TestStatus.ACTIVE,
                    TestStatus.INACTIVE
            );
        }

        @Test
        @DisplayName("알 수 없는 값은 무시한다")
        void ignoresUnknownValues() {
            Set<TestStatus> result = converter.convertToEntityAttribute(
                    "[\"ACTIVE\",\"UNKNOWN\",\"INVALID\"]"
            );
            assertThat(result).containsExactly(TestStatus.ACTIVE);
        }

        @Test
        @DisplayName("모든 값이 알 수 없으면 빈 Set 반환")
        void returnsEmptySetWhenAllUnknown() {
            Set<TestStatus> result = converter.convertToEntityAttribute("[\"UNKNOWN\",\"INVALID\"]");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("유효하지 않은 JSON은 예외를 발생시킨다")
        void throwsExceptionForInvalidJson() {
            assertThatThrownBy(() -> converter.convertToEntityAttribute("not a json"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to deserialize")
                    .hasMessageContaining("TestStatus");
        }
    }

    @Nested
    @DisplayName("양방향 변환 테스트")
    class RoundTripTest {

        @Test
        @DisplayName("Set → JSON → Set 변환이 동일한 결과를 반환한다")
        void roundTripConversion() {
            Set<TestStatus> original = EnumSet.of(
                    TestStatus.ACTIVE,
                    TestStatus.PENDING
            );

            String json = converter.convertToDatabaseColumn(original);
            Set<TestStatus> restored = converter.convertToEntityAttribute(json);

            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("빈 Set의 양방향 변환")
        void roundTripEmptySet() {
            Set<TestStatus> original = Collections.emptySet();

            String json = converter.convertToDatabaseColumn(original);
            Set<TestStatus> restored = converter.convertToEntityAttribute(json);

            assertThat(restored).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(TestStatus.class)
        @DisplayName("각 Enum 값의 양방향 변환")
        void roundTripEachValue(TestStatus status) {
            Set<TestStatus> original = EnumSet.of(status);

            String json = converter.convertToDatabaseColumn(original);
            Set<TestStatus> restored = converter.convertToEntityAttribute(json);

            assertThat(restored).containsExactly(status);
        }
    }

    @Nested
    @DisplayName("getEnumClass 테스트")
    class GetEnumClassTest {

        @Test
        @DisplayName("Enum 클래스를 반환한다")
        void returnsEnumClass() {
            assertThat(converter.getEnumClass()).isEqualTo(TestStatus.class);
        }
    }
}
