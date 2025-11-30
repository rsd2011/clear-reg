package com.example.common.orggroup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 조직그룹 관련 코드 타입 테스트.
 */
class OrgGroupCodeTypesTest {

    @Nested
    @DisplayName("WorkType")
    class WorkTypeTest {

        @Test
        @DisplayName("Given 대문자 코드 When fromString 호출 Then 해당 enum 반환")
        void fromString_uppercase_returns_enum() {
            assertThat(WorkType.fromString("GENERAL")).isEqualTo(WorkType.GENERAL);
            assertThat(WorkType.fromString("FILE_EXPORT")).isEqualTo(WorkType.FILE_EXPORT);
            assertThat(WorkType.fromString("DATA_CORRECTION")).isEqualTo(WorkType.DATA_CORRECTION);
            assertThat(WorkType.fromString("HR_UPDATE")).isEqualTo(WorkType.HR_UPDATE);
            assertThat(WorkType.fromString("POLICY_CHANGE")).isEqualTo(WorkType.POLICY_CHANGE);
        }

        @Test
        @DisplayName("Given 소문자 코드 When fromString 호출 Then 대소문자 무시하고 해당 enum 반환")
        void fromString_lowercase_returns_enum() {
            assertThat(WorkType.fromString("general")).isEqualTo(WorkType.GENERAL);
            assertThat(WorkType.fromString("file_export")).isEqualTo(WorkType.FILE_EXPORT);
        }

        @Test
        @DisplayName("Given 혼합 케이스 코드 When fromString 호출 Then 해당 enum 반환")
        void fromString_mixedCase_returns_enum() {
            assertThat(WorkType.fromString("General")).isEqualTo(WorkType.GENERAL);
            assertThat(WorkType.fromString("File_Export")).isEqualTo(WorkType.FILE_EXPORT);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("Given null 또는 빈 문자열 When fromString 호출 Then null 반환")
        void fromString_nullOrBlank_returns_null(String code) {
            assertThat(WorkType.fromString(code)).isNull();
        }

        @Test
        @DisplayName("Given 존재하지 않는 코드 When fromString 호출 Then null 반환")
        void fromString_invalid_returns_null() {
            assertThat(WorkType.fromString("INVALID_CODE")).isNull();
            assertThat(WorkType.fromString("UNKNOWN")).isNull();
        }

        @Test
        @DisplayName("모든 enum 상수가 정의되어 있다")
        void allConstants_exist() {
            assertThat(WorkType.values()).hasSize(5);
            assertThat(WorkType.values()).containsExactlyInAnyOrder(
                    WorkType.GENERAL,
                    WorkType.FILE_EXPORT,
                    WorkType.DATA_CORRECTION,
                    WorkType.HR_UPDATE,
                    WorkType.POLICY_CHANGE
            );
        }
    }

    @Nested
    @DisplayName("WorkCategory")
    class WorkCategoryTest {

        @Test
        @DisplayName("Given 대문자 코드 When fromString 호출 Then 해당 enum 반환")
        void fromString_uppercase_returns_enum() {
            assertThat(WorkCategory.fromString("COMPLIANCE")).isEqualTo(WorkCategory.COMPLIANCE);
            assertThat(WorkCategory.fromString("SALES")).isEqualTo(WorkCategory.SALES);
            assertThat(WorkCategory.fromString("TRADING")).isEqualTo(WorkCategory.TRADING);
            assertThat(WorkCategory.fromString("RISK_MANAGEMENT")).isEqualTo(WorkCategory.RISK_MANAGEMENT);
            assertThat(WorkCategory.fromString("OPERATIONS")).isEqualTo(WorkCategory.OPERATIONS);
        }

        @Test
        @DisplayName("Given 소문자 코드 When fromString 호출 Then 대소문자 무시하고 해당 enum 반환")
        void fromString_lowercase_returns_enum() {
            assertThat(WorkCategory.fromString("compliance")).isEqualTo(WorkCategory.COMPLIANCE);
            assertThat(WorkCategory.fromString("sales")).isEqualTo(WorkCategory.SALES);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("Given null 또는 빈 문자열 When fromString 호출 Then null 반환")
        void fromString_nullOrBlank_returns_null(String code) {
            assertThat(WorkCategory.fromString(code)).isNull();
        }

        @Test
        @DisplayName("Given 존재하지 않는 코드 When fromString 호출 Then null 반환")
        void fromString_invalid_returns_null() {
            assertThat(WorkCategory.fromString("INVALID_CODE")).isNull();
        }

        @Test
        @DisplayName("모든 enum 상수가 정의되어 있다")
        void allConstants_exist() {
            assertThat(WorkCategory.values()).hasSize(5);
            assertThat(WorkCategory.values()).containsExactlyInAnyOrder(
                    WorkCategory.COMPLIANCE,
                    WorkCategory.SALES,
                    WorkCategory.TRADING,
                    WorkCategory.RISK_MANAGEMENT,
                    WorkCategory.OPERATIONS
            );
        }
    }

    @Nested
    @DisplayName("OrgGroupRoleType")
    class OrgGroupRoleTypeTest {

        @Test
        @DisplayName("Given 대문자 코드 When fromString 호출 Then 해당 enum 반환")
        void fromString_uppercase_returns_enum() {
            assertThat(OrgGroupRoleType.fromString("LEADER")).isEqualTo(OrgGroupRoleType.LEADER);
            assertThat(OrgGroupRoleType.fromString("MANAGER")).isEqualTo(OrgGroupRoleType.MANAGER);
            assertThat(OrgGroupRoleType.fromString("MEMBER")).isEqualTo(OrgGroupRoleType.MEMBER);
        }

        @Test
        @DisplayName("Given 소문자 코드 When fromString 호출 Then 대소문자 무시하고 해당 enum 반환")
        void fromString_lowercase_returns_enum() {
            assertThat(OrgGroupRoleType.fromString("leader")).isEqualTo(OrgGroupRoleType.LEADER);
            assertThat(OrgGroupRoleType.fromString("manager")).isEqualTo(OrgGroupRoleType.MANAGER);
            assertThat(OrgGroupRoleType.fromString("member")).isEqualTo(OrgGroupRoleType.MEMBER);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("Given null 또는 빈 문자열 When fromString 호출 Then null 반환")
        void fromString_nullOrBlank_returns_null(String code) {
            assertThat(OrgGroupRoleType.fromString(code)).isNull();
        }

        @Test
        @DisplayName("Given 존재하지 않는 코드 When fromString 호출 Then null 반환")
        void fromString_invalid_returns_null() {
            assertThat(OrgGroupRoleType.fromString("INVALID_CODE")).isNull();
            assertThat(OrgGroupRoleType.fromString("ADMIN")).isNull();
        }

        @Test
        @DisplayName("모든 enum 상수가 정의되어 있다")
        void allConstants_exist() {
            assertThat(OrgGroupRoleType.values()).hasSize(3);
            assertThat(OrgGroupRoleType.values()).containsExactlyInAnyOrder(
                    OrgGroupRoleType.LEADER,
                    OrgGroupRoleType.MANAGER,
                    OrgGroupRoleType.MEMBER
            );
        }
    }
}
