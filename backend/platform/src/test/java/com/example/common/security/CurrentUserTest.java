package com.example.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CurrentUser 테스트")
class CurrentUserTest {

    @Nested
    @DisplayName("전체 필드 생성자")
    class FullConstructor {

        @Test
        @DisplayName("Given 모든 필드 When 생성하면 Then 모든 필드가 올바르게 설정된다")
        void createCurrentUserWithAllFields() {
            CurrentUser user = new CurrentUser(
                    "testUser",
                    "ORG1",
                    "ADMIN_GROUP",
                    "ORGANIZATION",
                    "READ",
                    RowScope.ORG,
                    List.of("GROUP1", "GROUP2")
            );

            assertThat(user.username()).isEqualTo("testUser");
            assertThat(user.organizationCode()).isEqualTo("ORG1");
            assertThat(user.permissionGroupCode()).isEqualTo("ADMIN_GROUP");
            assertThat(user.featureCode()).isEqualTo("ORGANIZATION");
            assertThat(user.actionCode()).isEqualTo("READ");
            assertThat(user.rowScope()).isEqualTo(RowScope.ORG);
            assertThat(user.orgGroupCodes()).containsExactly("GROUP1", "GROUP2");
        }

        @Test
        @DisplayName("Given null 값들 When 생성하면 Then null 필드가 허용된다")
        void createCurrentUserWithNullValues() {
            CurrentUser user = new CurrentUser(
                    "user",
                    "ORG",
                    null,
                    null,
                    null,
                    RowScope.OWN,
                    List.of()
            );

            assertThat(user.username()).isEqualTo("user");
            assertThat(user.orgGroupCodes()).isEmpty();
        }

        @Test
        @DisplayName("Given null orgGroupCodes When 생성하면 Then 빈 리스트로 변환된다")
        void nullOrgGroupCodesConvertedToEmptyList() {
            CurrentUser user = new CurrentUser(
                    "user",
                    "ORG",
                    "GROUP",
                    "DRAFT",
                    "CREATE",
                    RowScope.ALL,
                    null
            );

            assertThat(user.orgGroupCodes()).isEmpty();
            assertThat(user.orgGroupCodes()).isNotNull();
        }

        @Test
        @DisplayName("Given mutable orgGroupCodes When 생성하면 Then 방어적 복사가 수행된다")
        void orgGroupCodesAreDefensivelyCopied() {
            List<String> mutableList = new ArrayList<>();
            mutableList.add("GROUP1");

            CurrentUser user = new CurrentUser(
                    "user",
                    "ORG",
                    "GROUP",
                    "DRAFT",
                    "CREATE",
                    RowScope.ALL,
                    mutableList
            );

            mutableList.add("GROUP2");

            assertThat(user.orgGroupCodes()).containsExactly("GROUP1");
            assertThat(user.orgGroupCodes()).hasSize(1);
        }

        @Test
        @DisplayName("Given orgGroupCodes When 수정 시도하면 Then UnsupportedOperationException 발생")
        void orgGroupCodesAreImmutable() {
            CurrentUser user = new CurrentUser(
                    "user",
                    "ORG",
                    "GROUP",
                    "DRAFT",
                    "CREATE",
                    RowScope.ALL,
                    List.of("GROUP1")
            );

            List<String> codes = user.orgGroupCodes();

            assertThatThrownBy(() -> codes.add("NEW"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("역호환성 생성자")
    class LegacyConstructor {

        @Test
        @DisplayName("Given 6개 파라미터 When 역호환성 생성자 사용하면 Then 기본값이 설정된다")
        void legacyConstructorSetsDefaults() {
            CurrentUser user = new CurrentUser(
                    "legacyUser",
                    "ORG_LEGACY",
                    "LEGACY_GROUP",
                    "FEATURE",
                    "ACTION",
                    RowScope.OWN
            );

            assertThat(user.username()).isEqualTo("legacyUser");
            assertThat(user.organizationCode()).isEqualTo("ORG_LEGACY");
            assertThat(user.permissionGroupCode()).isEqualTo("LEGACY_GROUP");
            assertThat(user.featureCode()).isEqualTo("FEATURE");
            assertThat(user.actionCode()).isEqualTo("ACTION");
            assertThat(user.rowScope()).isEqualTo(RowScope.OWN);
            assertThat(user.orgGroupCodes()).isEmpty();
        }

        @Test
        @DisplayName("Given 역호환성 생성자 When RowScope 설정하면 Then 올바르게 저장된다")
        void legacyConstructorWithDifferentRowScopes() {
            CurrentUser userAll = new CurrentUser("u1", "O1", "G1", "F1", "A1", RowScope.ALL);
            CurrentUser userOrg = new CurrentUser("u2", "O2", "G2", "F2", "A2", RowScope.ORG);

            assertThat(userAll.rowScope()).isEqualTo(RowScope.ALL);
            assertThat(userOrg.rowScope()).isEqualTo(RowScope.ORG);
        }
    }

    @Nested
    @DisplayName("레코드 동등성")
    class RecordEquality {

        @Test
        @DisplayName("Given 동일한 값 When equals 호출하면 Then true 반환")
        void equalValuesAreEqual() {
            CurrentUser user1 = new CurrentUser(
                    "user", "ORG", "GROUP", "DRAFT", "CREATE",
                    RowScope.ALL, List.of("G1")
            );
            CurrentUser user2 = new CurrentUser(
                    "user", "ORG", "GROUP", "DRAFT", "CREATE",
                    RowScope.ALL, List.of("G1")
            );

            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("Given 다른 값 When equals 호출하면 Then false 반환")
        void differentValuesAreNotEqual() {
            CurrentUser user1 = new CurrentUser(
                    "user1", "ORG", "GROUP", "DRAFT", "CREATE",
                    RowScope.ALL, List.of("G1")
            );
            CurrentUser user2 = new CurrentUser(
                    "user2", "ORG", "GROUP", "DRAFT", "CREATE",
                    RowScope.ALL, List.of("G1")
            );

            assertThat(user1).isNotEqualTo(user2);
        }
    }
}
