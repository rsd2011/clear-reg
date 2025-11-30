package com.example.admin.permission.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.domain.PermissionGroupRoot;
import com.example.admin.permission.spi.UserInfo;
import com.example.common.version.ChangeAction;

/**
 * PermissionDecision 테스트.
 *
 * <p>RowScope는 RowAccessPolicy로 이관되어 PermissionAssignment에서 제거되었습니다.
 */
@DisplayName("PermissionDecision 테스트")
class PermissionDecisionTest {

    @Nested
    @DisplayName("객체 생성")
    class Creation {

        @Test
        @DisplayName("Given 모든 필드 When 생성자 호출하면 Then 모든 필드가 설정된다")
        void allFieldsAreSet() {
            UserInfo userInfo = createUserInfo("user1", "ORG001", "GROUP001");
            PermissionAssignment assignment = new PermissionAssignment(
                    FeatureCode.ORGANIZATION, ActionCode.READ);
            PermissionGroup group = createTestGroup("GROUP001", "테스트 그룹");

            PermissionDecision decision = new PermissionDecision(userInfo, assignment, group);

            assertThat(decision.userInfo()).isEqualTo(userInfo);
            assertThat(decision.assignment()).isEqualTo(assignment);
            assertThat(decision.group()).isEqualTo(group);
        }

        @Test
        @DisplayName("Given null 필드 When 생성자 호출하면 Then null이 허용된다")
        void nullFieldsAreAllowed() {
            PermissionDecision decision = new PermissionDecision(null, null, null);

            assertThat(decision.userInfo()).isNull();
            assertThat(decision.assignment()).isNull();
            assertThat(decision.group()).isNull();
        }
    }

    @Nested
    @DisplayName("toContext 변환")
    class ToContext {

        @Test
        @DisplayName("Given 권한 결정 When toContext 호출하면 Then AuthContext가 생성된다")
        void convertsToAuthContext() {
            UserInfo userInfo = createUserInfo("testUser", "ORG100", "GRP100");
            PermissionAssignment assignment = new PermissionAssignment(
                    FeatureCode.DRAFT, ActionCode.UPDATE);
            PermissionGroup group = createTestGroup("GRP100", "그룹");

            PermissionDecision decision = new PermissionDecision(userInfo, assignment, group);
            AuthContext context = decision.toContext();

            assertThat(context.username()).isEqualTo("testUser");
            assertThat(context.organizationCode()).isEqualTo("ORG100");
            assertThat(context.permissionGroupCode()).isEqualTo("GRP100");
            assertThat(context.feature()).isEqualTo(FeatureCode.DRAFT);
            assertThat(context.action()).isEqualTo(ActionCode.UPDATE);
        }
    }

    @Nested
    @DisplayName("equals 메서드")
    class Equals {

        @Test
        @DisplayName("Given 동일한 객체 When equals 호출하면 Then true 반환")
        void sameObjectReturnsTrue() {
            PermissionDecision decision = createDecision();

            assertThat(decision.equals(decision)).isTrue();
        }

        @Test
        @DisplayName("Given 동일한 값 When equals 호출하면 Then true 반환")
        void equalValuesReturnsTrue() {
            UserInfo userInfo = createUserInfo("user", "org", "grp");
            PermissionAssignment assignment = new PermissionAssignment(
                    FeatureCode.ORGANIZATION, ActionCode.READ);
            PermissionGroup group = createTestGroup("grp", "그룹");

            PermissionDecision decision1 = new PermissionDecision(userInfo, assignment, group);
            PermissionDecision decision2 = new PermissionDecision(userInfo, assignment, group);

            assertThat(decision1.equals(decision2)).isTrue();
        }

        @Test
        @DisplayName("Given 다른 타입 When equals 호출하면 Then false 반환")
        void differentTypeReturnsFalse() {
            PermissionDecision decision = createDecision();

            assertThat(decision.equals("not a decision")).isFalse();
        }

        @Test
        @DisplayName("Given null When equals 호출하면 Then false 반환")
        void nullReturnsFalse() {
            PermissionDecision decision = createDecision();

            assertThat(decision.equals(null)).isFalse();
        }

        @Test
        @DisplayName("Given 다른 userInfo When equals 호출하면 Then false 반환")
        void differentUserInfoReturnsFalse() {
            UserInfo userInfo1 = createUserInfo("user1", "org", "grp");
            UserInfo userInfo2 = createUserInfo("user2", "org", "grp");
            PermissionAssignment assignment = new PermissionAssignment(
                    FeatureCode.ORGANIZATION, ActionCode.READ);
            PermissionGroup group = createTestGroup("grp", "그룹");

            PermissionDecision decision1 = new PermissionDecision(userInfo1, assignment, group);
            PermissionDecision decision2 = new PermissionDecision(userInfo2, assignment, group);

            assertThat(decision1.equals(decision2)).isFalse();
        }
    }

    @Nested
    @DisplayName("hashCode 메서드")
    class HashCode {

        @Test
        @DisplayName("Given 동일한 값 When hashCode 호출하면 Then 같은 해시코드 반환")
        void equalObjectsHaveSameHashCode() {
            UserInfo userInfo = createUserInfo("user", "org", "grp");
            PermissionAssignment assignment = new PermissionAssignment(
                    FeatureCode.ORGANIZATION, ActionCode.READ);
            PermissionGroup group = createTestGroup("grp", "그룹");

            PermissionDecision decision1 = new PermissionDecision(userInfo, assignment, group);
            PermissionDecision decision2 = new PermissionDecision(userInfo, assignment, group);

            assertThat(decision1.hashCode()).isEqualTo(decision2.hashCode());
        }

        @Test
        @DisplayName("Given null 필드 When hashCode 호출하면 Then 예외 없이 해시코드 반환")
        void nullFieldsDoNotCauseException() {
            PermissionDecision decision = new PermissionDecision(null, null, null);

            assertThat(decision.hashCode()).isNotNull();
        }
    }

    @Nested
    @DisplayName("toString 메서드")
    class ToString {

        @Test
        @DisplayName("Given 권한 결정 When toString 호출하면 Then 모든 필드가 포함된 문자열 반환")
        void includesAllFields() {
            UserInfo userInfo = createUserInfo("testUser", "ORG001", "GRP001");
            PermissionAssignment assignment = new PermissionAssignment(
                    FeatureCode.DRAFT, ActionCode.UPDATE);
            PermissionGroup group = createTestGroup("GRP001", "테스트 그룹");

            PermissionDecision decision = new PermissionDecision(userInfo, assignment, group);
            String result = decision.toString();

            assertThat(result)
                    .contains("PermissionDecision")
                    .contains("userInfo=")
                    .contains("assignment=")
                    .contains("group=");
        }
    }

    private PermissionDecision createDecision() {
        UserInfo userInfo = createUserInfo("user", "org", "grp");
        PermissionAssignment assignment = new PermissionAssignment(
                FeatureCode.ORGANIZATION, ActionCode.READ);
        PermissionGroup group = createTestGroup("grp", "그룹");
        return new PermissionDecision(userInfo, assignment, group);
    }

    private PermissionGroup createTestGroup(String code, String name) {
        OffsetDateTime now = OffsetDateTime.now();
        PermissionGroupRoot root = PermissionGroupRoot.createWithCode(code, now);
        return PermissionGroup.create(
                root,
                1,
                name,
                "테스트용 그룹",
                true,
                List.of(),
                List.of(),
                ChangeAction.CREATE,
                "테스트 생성",
                "SYSTEM",
                "System",
                now);
    }

    private UserInfo createUserInfo(String username, String orgCode, String groupCode) {
        return new UserInfo() {
            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getOrganizationCode() {
                return orgCode;
            }

            @Override
            public String getPermissionGroupCode() {
                return groupCode;
            }

            @Override
            public Set<String> getRoles() {
                return Set.of("ROLE_USER");
            }
        };
    }
}
