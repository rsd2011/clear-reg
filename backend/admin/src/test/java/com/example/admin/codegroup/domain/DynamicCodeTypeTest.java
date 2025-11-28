package com.example.admin.codegroup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * DynamicCodeType 테스트.
 */
@DisplayName("DynamicCodeType 테스트")
class DynamicCodeTypeTest {

    @Nested
    @DisplayName("getSource 테스트")
    class GetSourceTests {

        @ParameterizedTest
        @EnumSource(DynamicCodeType.class)
        @DisplayName("Given: 모든 DynamicCodeType / When: getSource 호출 / Then: DYNAMIC_DB 반환")
        void returnsDynamicDbSourceForAllTypes(DynamicCodeType type) {
            // When
            CodeGroupSource source = type.getSource();

            // Then
            assertThat(source).isEqualTo(CodeGroupSource.DYNAMIC_DB);
        }
    }

    @Nested
    @DisplayName("fromCode 테스트")
    class FromCodeTests {

        @Test
        @DisplayName("Given: 정확한 코드 / When: fromCode 호출 / Then: 해당 타입 반환")
        void returnsTypeForExactCode() {
            // When
            DynamicCodeType result = DynamicCodeType.fromCode("NOTICE_CATEGORY");

            // Then
            assertThat(result).isEqualTo(DynamicCodeType.NOTICE_CATEGORY);
        }

        @Test
        @DisplayName("Given: 소문자 코드 / When: fromCode 호출 / Then: 해당 타입 반환 (case insensitive)")
        void returnsTypeForLowercaseCode() {
            // When
            DynamicCodeType result = DynamicCodeType.fromCode("alert_channel");

            // Then
            assertThat(result).isEqualTo(DynamicCodeType.ALERT_CHANNEL);
        }

        @Test
        @DisplayName("Given: 혼합 케이스 코드 / When: fromCode 호출 / Then: 해당 타입 반환")
        void returnsTypeForMixedCaseCode() {
            // When
            DynamicCodeType result = DynamicCodeType.fromCode("Custom");

            // Then
            assertThat(result).isEqualTo(DynamicCodeType.CUSTOM);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Given: null 또는 빈 코드 / When: fromCode 호출 / Then: null 반환")
        void returnsNullForNullOrEmptyCode(String code) {
            // When
            DynamicCodeType result = DynamicCodeType.fromCode(code);

            // Then
            assertThat(result).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"INVALID", "NOT_EXIST", "STATIC_ENUM"})
        @DisplayName("Given: 존재하지 않는 코드 / When: fromCode 호출 / Then: null 반환")
        void returnsNullForInvalidCode(String code) {
            // When
            DynamicCodeType result = DynamicCodeType.fromCode(code);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("isDynamicType 테스트")
    class IsDynamicTypeTests {

        @ParameterizedTest
        @ValueSource(strings = {"NOTICE_CATEGORY", "ALERT_CHANNEL", "CUSTOM", "notice_category"})
        @DisplayName("Given: 동적 코드 타입 / When: isDynamicType 호출 / Then: true 반환")
        void returnsTrueForDynamicTypes(String code) {
            // When
            boolean result = DynamicCodeType.isDynamicType(code);

            // Then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"INVALID", "STATIC_ENUM", "APPROVAL_STATUS"})
        @DisplayName("Given: 비동적 코드 타입 / When: isDynamicType 호출 / Then: false 반환")
        void returnsFalseForNonDynamicTypes(String code) {
            // When
            boolean result = DynamicCodeType.isDynamicType(code);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Given: null / When: isDynamicType 호출 / Then: false 반환")
        void returnsFalseForNull() {
            // When
            boolean result = DynamicCodeType.isDynamicType(null);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("enum 값 테스트")
    class EnumValuesTests {

        @Test
        @DisplayName("Given: DynamicCodeType / When: values 호출 / Then: 3개 타입 존재")
        void hasThreeTypes() {
            // When
            DynamicCodeType[] values = DynamicCodeType.values();

            // Then
            assertThat(values).hasSize(3);
            assertThat(values).containsExactly(
                    DynamicCodeType.NOTICE_CATEGORY,
                    DynamicCodeType.ALERT_CHANNEL,
                    DynamicCodeType.CUSTOM
            );
        }
    }
}
