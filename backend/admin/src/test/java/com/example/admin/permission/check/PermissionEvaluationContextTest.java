package com.example.admin.permission.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.PermissionAssignment;
import com.example.admin.permission.PermissionGroup;
import com.example.admin.permission.spi.UserInfo;
import com.example.common.security.RowScope;

@DisplayName("PermissionEvaluationContext 테스트")
class PermissionEvaluationContextTest {

    @Nested
    @DisplayName("객체 생성")
    class Creation {

        @Test
        @DisplayName("Given 모든 필드 When 생성자 호출하면 Then 모든 필드가 설정된다")
        void allFieldsAreSet() {
            FeatureCode feature = FeatureCode.ORGANIZATION;
            ActionCode action = ActionCode.READ;
            UserInfo userInfo = createUserInfo("user1", "ORG001", "GROUP001");
            PermissionGroup group = new PermissionGroup("GROUP001", "테스트 그룹");
            PermissionAssignment assignment = new PermissionAssignment(
                    feature, action, RowScope.ALL);
            Map<String, Object> attributes = Map.of("key1", "value1", "key2", 123);

            PermissionEvaluationContext context = new PermissionEvaluationContext(
                    feature, action, userInfo, group, assignment, attributes);

            assertThat(context.feature()).isEqualTo(feature);
            assertThat(context.action()).isEqualTo(action);
            assertThat(context.userInfo()).isEqualTo(userInfo);
            assertThat(context.group()).isEqualTo(group);
            assertThat(context.assignment()).isEqualTo(assignment);
            assertThat(context.attributes()).containsEntry("key1", "value1");
            assertThat(context.attributes()).containsEntry("key2", 123);
        }

        @Test
        @DisplayName("Given 빈 attributes When 생성자 호출하면 Then 빈 Map이 설정된다")
        void emptyAttributesAreAllowed() {
            PermissionEvaluationContext context = new PermissionEvaluationContext(
                    FeatureCode.DRAFT,
                    ActionCode.CREATE,
                    createUserInfo("user", "org", "grp"),
                    new PermissionGroup("grp", "그룹"),
                    new PermissionAssignment(FeatureCode.DRAFT, ActionCode.CREATE, RowScope.OWN),
                    Map.of());

            assertThat(context.attributes()).isEmpty();
        }

        @Test
        @DisplayName("Given null 필드들 When 생성자 호출하면 Then null이 허용된다")
        void nullFieldsAreAllowed() {
            PermissionEvaluationContext context = new PermissionEvaluationContext(
                    null, null, null, null, null, Map.of());

            assertThat(context.feature()).isNull();
            assertThat(context.action()).isNull();
            assertThat(context.userInfo()).isNull();
            assertThat(context.group()).isNull();
            assertThat(context.assignment()).isNull();
            assertThat(context.attributes()).isEmpty();
        }

        @Test
        @DisplayName("Given null attributes When 생성자 호출하면 Then NullPointerException 발생")
        void nullAttributesThrowsException() {
            assertThatThrownBy(() -> new PermissionEvaluationContext(
                    FeatureCode.DRAFT, ActionCode.CREATE, null, null, null, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("attributes 불변성")
    class AttributesImmutability {

        @Test
        @DisplayName("Given context 생성 후 When attributes 수정 시도하면 Then UnsupportedOperationException 발생")
        void attributesAreImmutable() {
            Map<String, Object> originalAttributes = Map.of("key", "value");
            PermissionEvaluationContext context = new PermissionEvaluationContext(
                    FeatureCode.ORGANIZATION,
                    ActionCode.READ,
                    createUserInfo("user", "org", "grp"),
                    new PermissionGroup("grp", "그룹"),
                    new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL),
                    originalAttributes);

            Map<String, Object> returnedAttributes = context.attributes();

            assertThatThrownBy(() -> returnedAttributes.put("newKey", "newValue"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("접근자 메서드")
    class Accessors {

        @Test
        @DisplayName("Given 다양한 FeatureCode When feature() 호출하면 Then 올바른 값 반환")
        void featureAccessorReturnsCorrectValue() {
            PermissionEvaluationContext context1 = createContext(FeatureCode.DRAFT, ActionCode.CREATE);
            PermissionEvaluationContext context2 = createContext(FeatureCode.ORGANIZATION, ActionCode.READ);
            PermissionEvaluationContext context3 = createContext(FeatureCode.APPROVAL, ActionCode.APPROVE);

            assertThat(context1.feature()).isEqualTo(FeatureCode.DRAFT);
            assertThat(context2.feature()).isEqualTo(FeatureCode.ORGANIZATION);
            assertThat(context3.feature()).isEqualTo(FeatureCode.APPROVAL);
        }

        @Test
        @DisplayName("Given 다양한 ActionCode When action() 호출하면 Then 올바른 값 반환")
        void actionAccessorReturnsCorrectValue() {
            PermissionEvaluationContext context1 = createContext(FeatureCode.DRAFT, ActionCode.CREATE);
            PermissionEvaluationContext context2 = createContext(FeatureCode.DRAFT, ActionCode.READ);
            PermissionEvaluationContext context3 = createContext(FeatureCode.DRAFT, ActionCode.UPDATE);
            PermissionEvaluationContext context4 = createContext(FeatureCode.DRAFT, ActionCode.DELETE);

            assertThat(context1.action()).isEqualTo(ActionCode.CREATE);
            assertThat(context2.action()).isEqualTo(ActionCode.READ);
            assertThat(context3.action()).isEqualTo(ActionCode.UPDATE);
            assertThat(context4.action()).isEqualTo(ActionCode.DELETE);
        }

        @Test
        @DisplayName("Given PermissionGroup 설정 When group() 호출하면 Then 올바른 그룹 반환")
        void groupAccessorReturnsCorrectValue() {
            PermissionGroup expectedGroup = new PermissionGroup("ADMIN_GROUP", "관리자 그룹");
            PermissionEvaluationContext context = new PermissionEvaluationContext(
                    FeatureCode.ORGANIZATION,
                    ActionCode.READ,
                    createUserInfo("admin", "HQ", "ADMIN_GROUP"),
                    expectedGroup,
                    new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL),
                    Map.of());

            assertThat(context.group()).isEqualTo(expectedGroup);
            assertThat(context.group().getCode()).isEqualTo("ADMIN_GROUP");
            assertThat(context.group().getName()).isEqualTo("관리자 그룹");
        }

        @Test
        @DisplayName("Given PermissionAssignment 설정 When assignment() 호출하면 Then 올바른 할당 반환")
        void assignmentAccessorReturnsCorrectValue() {
            PermissionAssignment expectedAssignment = new PermissionAssignment(
                    FeatureCode.DRAFT, ActionCode.UPDATE, RowScope.ORG);
            PermissionEvaluationContext context = new PermissionEvaluationContext(
                    FeatureCode.DRAFT,
                    ActionCode.UPDATE,
                    createUserInfo("user", "org", "grp"),
                    new PermissionGroup("grp", "그룹"),
                    expectedAssignment,
                    Map.of());

            assertThat(context.assignment()).isEqualTo(expectedAssignment);
            assertThat(context.assignment().getRowScope()).isEqualTo(RowScope.ORG);
        }
    }

    private PermissionEvaluationContext createContext(FeatureCode feature, ActionCode action) {
        return new PermissionEvaluationContext(
                feature,
                action,
                createUserInfo("user", "org", "grp"),
                new PermissionGroup("grp", "그룹"),
                new PermissionAssignment(feature, action, RowScope.OWN),
                Map.of());
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
