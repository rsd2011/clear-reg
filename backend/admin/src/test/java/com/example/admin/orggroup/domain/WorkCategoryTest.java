package com.example.admin.orggroup.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.common.orggroup.WorkCategory;

@DisplayName("WorkCategory 테스트")
class WorkCategoryTest {

    @Nested
    @DisplayName("enum 값 테스트")
    class EnumValuesTest {

        @Test
        @DisplayName("모든 업무 카테고리가 정의되어 있다")
        void allCategoriesAreDefined() {
            assertThat(WorkCategory.values()).containsExactlyInAnyOrder(
                    WorkCategory.COMPLIANCE,
                    WorkCategory.SALES,
                    WorkCategory.TRADING,
                    WorkCategory.RISK_MANAGEMENT,
                    WorkCategory.OPERATIONS
            );
        }

        @ParameterizedTest
        @EnumSource(WorkCategory.class)
        @DisplayName("각 카테고리는 고유한 name을 가진다")
        void eachCategoryHasUniqueName(WorkCategory category) {
            assertThat(category.name()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("fromString 테스트")
    class FromStringTest {

        @ParameterizedTest
        @EnumSource(WorkCategory.class)
        @DisplayName("대문자 코드로 WorkCategory를 찾을 수 있다")
        void findsFromUpperCaseCode(WorkCategory expected) {
            // given
            String code = expected.name();

            // when
            WorkCategory result = WorkCategory.fromString(code);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest
        @EnumSource(WorkCategory.class)
        @DisplayName("소문자 코드로도 WorkCategory를 찾을 수 있다")
        void findsFromLowerCaseCode(WorkCategory expected) {
            // given
            String code = expected.name().toLowerCase();

            // when
            WorkCategory result = WorkCategory.fromString(code);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("혼합된 대소문자 코드로도 찾을 수 있다")
        void findsFromMixedCaseCode() {
            assertThat(WorkCategory.fromString("Compliance")).isEqualTo(WorkCategory.COMPLIANCE);
            assertThat(WorkCategory.fromString("Risk_Management")).isEqualTo(WorkCategory.RISK_MANAGEMENT);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("null 또는 공백 문자열은 null을 반환한다")
        void returnsNullForNullOrBlank(String code) {
            assertThat(WorkCategory.fromString(code)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"UNKNOWN", "invalid", "NOT_EXISTS", "123"})
        @DisplayName("존재하지 않는 코드는 null을 반환한다")
        void returnsNullForInvalidCode(String code) {
            assertThat(WorkCategory.fromString(code)).isNull();
        }
    }
}
