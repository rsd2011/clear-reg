package com.example.admin.codegroup.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * CodeGroupUtils 테스트.
 */
@DisplayName("CodeGroupUtils 테스트")
class CodeGroupUtilsTest {

    @Nested
    @DisplayName("toGroupCode(String) 테스트")
    class ToGroupCodeFromStringTests {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "ApprovalStatus, APPROVAL_STATUS",
                "DraftType, DRAFT_TYPE",
                "HTTPStatus, HTTP_STATUS",
                "XMLParser, XML_PARSER",
                "ID, ID",
                "URLBuilder, URL_BUILDER",
                "ABC, ABC",
                "AbCdEf, AB_CD_EF",
                "status, STATUS",
                "SimpleClass, SIMPLE_CLASS",
                "A, A",
                "AB, AB"
        })
        @DisplayName("Given: PascalCase 문자열 / When: toGroupCode 호출 / Then: UPPER_SNAKE_CASE 반환")
        void convertsToUpperSnakeCase(String input, String expected) {
            // When
            String result = CodeGroupUtils.toGroupCode(input);

            // Then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Given: null 또는 빈 문자열 / When: toGroupCode 호출 / Then: 그대로 반환")
        void returnsNullOrEmptyAsIs(String input) {
            // When
            String result = CodeGroupUtils.toGroupCode(input);

            // Then
            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("toGroupCode(Class) 테스트")
    class ToGroupCodeFromClassTests {

        @Test
        @DisplayName("Given: Enum 클래스 / When: toGroupCode 호출 / Then: 클래스명 기반 UPPER_SNAKE_CASE 반환")
        void convertsEnumClassToGroupCode() {
            // When
            String result = CodeGroupUtils.toGroupCode(TestStatus.class);

            // Then
            assertThat(result).isEqualTo("TEST_STATUS");
        }

        @Test
        @DisplayName("Given: 단일 단어 Enum / When: toGroupCode 호출 / Then: 대문자로 반환")
        void convertsSingleWordEnum() {
            // When
            String result = CodeGroupUtils.toGroupCode(Status.class);

            // Then
            assertThat(result).isEqualTo("STATUS");
        }

        // 테스트용 Enum 클래스
        private enum TestStatus { ACTIVE, INACTIVE }
        private enum Status { ACTIVE }
    }

    @Nested
    @DisplayName("normalize 테스트")
    class NormalizeTests {

        @Test
        @DisplayName("Given: 소문자 문자열 / When: normalize 호출 / Then: 대문자로 반환")
        void normalizesLowercase() {
            // When
            String result = CodeGroupUtils.normalize("hello");

            // Then
            assertThat(result).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("Given: 대소문자 혼합 문자열 / When: normalize 호출 / Then: 대문자로 반환")
        void normalizesMixedCase() {
            // When
            String result = CodeGroupUtils.normalize("HelloWorld");

            // Then
            assertThat(result).isEqualTo("HELLOWORLD");
        }

        @Test
        @DisplayName("Given: null / When: normalize 호출 / Then: null 반환")
        void returnsNullForNull() {
            // When
            String result = CodeGroupUtils.normalize(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Given: 빈 문자열 / When: normalize 호출 / Then: 빈 문자열 반환")
        void returnsEmptyForEmpty() {
            // When
            String result = CodeGroupUtils.normalize("");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: 대문자 문자열 / When: normalize 호출 / Then: 그대로 반환")
        void returnsUppercaseAsIs() {
            // When
            String result = CodeGroupUtils.normalize("ALREADY_UPPER");

            // Then
            assertThat(result).isEqualTo("ALREADY_UPPER");
        }

        @Test
        @DisplayName("Given: 특수문자 포함 문자열 / When: normalize 호출 / Then: 대문자로 반환")
        void normalizesWithSpecialChars() {
            // When
            String result = CodeGroupUtils.normalize("hello_world-123");

            // Then
            assertThat(result).isEqualTo("HELLO_WORLD-123");
        }
    }
}
