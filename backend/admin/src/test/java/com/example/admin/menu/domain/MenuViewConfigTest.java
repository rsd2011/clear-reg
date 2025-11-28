package com.example.admin.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.admin.menu.domain.MenuViewConfig.TargetType;
import com.example.admin.menu.domain.MenuViewConfig.VisibilityAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * MenuViewConfig 단위 테스트.
 */
class MenuViewConfigTest {

    private Menu createTestMenu() {
        return new Menu("TEST_MENU", "테스트 메뉴");
    }

    @Nested
    @DisplayName("팩토리 메서드")
    class FactoryMethodTest {

        @Test
        @DisplayName("Given 권한 그룹 정보 - When forPermissionGroup - Then PERMISSION_GROUP 타입 생성")
        void forPermissionGroup_validInput_createsConfig() {
            // Given
            Menu menu = createTestMenu();
            String groupCode = "ADMIN_GROUP";

            // When
            MenuViewConfig config = MenuViewConfig.forPermissionGroup(
                    menu, groupCode, VisibilityAction.SHOW);

            // Then
            assertThat(config.getMenu()).isEqualTo(menu);
            assertThat(config.getTargetType()).isEqualTo(TargetType.PERMISSION_GROUP);
            assertThat(config.getPermissionGroupCode()).isEqualTo(groupCode);
            assertThat(config.getOrgPolicyId()).isNull();
            assertThat(config.getVisibilityAction()).isEqualTo(VisibilityAction.SHOW);
        }

        @Test
        @DisplayName("Given 조직 정책 정보 - When forOrgPolicy - Then ORG_POLICY 타입 생성")
        void forOrgPolicy_validInput_createsConfig() {
            // Given
            Menu menu = createTestMenu();
            Long orgPolicyId = 123L;

            // When
            MenuViewConfig config = MenuViewConfig.forOrgPolicy(
                    menu, orgPolicyId, VisibilityAction.HIDE);

            // Then
            assertThat(config.getTargetType()).isEqualTo(TargetType.ORG_POLICY);
            assertThat(config.getOrgPolicyId()).isEqualTo(orgPolicyId);
            assertThat(config.getPermissionGroupCode()).isNull();
            assertThat(config.getVisibilityAction()).isEqualTo(VisibilityAction.HIDE);
        }

        @Test
        @DisplayName("Given 글로벌 설정 요청 - When forGlobal - Then GLOBAL 타입 생성")
        void forGlobal_validInput_createsConfig() {
            // Given
            Menu menu = createTestMenu();

            // When
            MenuViewConfig config = MenuViewConfig.forGlobal(menu, VisibilityAction.HIGHLIGHT);

            // Then
            assertThat(config.getTargetType()).isEqualTo(TargetType.GLOBAL);
            assertThat(config.getPermissionGroupCode()).isNull();
            assertThat(config.getOrgPolicyId()).isNull();
            assertThat(config.getVisibilityAction()).isEqualTo(VisibilityAction.HIGHLIGHT);
        }

        @Test
        @DisplayName("Given null 메뉴 - When forPermissionGroup - Then NullPointerException")
        void forPermissionGroup_nullMenu_throwsException() {
            assertThatThrownBy(() ->
                    MenuViewConfig.forPermissionGroup(null, "GROUP", VisibilityAction.SHOW))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Given null 그룹코드 - When forPermissionGroup - Then NullPointerException")
        void forPermissionGroup_nullGroupCode_throwsException() {
            Menu menu = createTestMenu();

            assertThatThrownBy(() ->
                    MenuViewConfig.forPermissionGroup(menu, null, VisibilityAction.SHOW))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("appliesTo 메서드 - 권한 그룹")
    class AppliesToPermissionGroupTest {

        @Test
        @DisplayName("Given GLOBAL 타입 - When appliesTo(permGroupCode) - Then true")
        void appliesTo_globalType_alwaysTrue() {
            // Given
            MenuViewConfig config = MenuViewConfig.forGlobal(createTestMenu(), VisibilityAction.SHOW);

            // When & Then
            assertThat(config.appliesTo("ANY_GROUP")).isTrue();
            assertThat(config.appliesTo((String) null)).isTrue();
        }

        @Test
        @DisplayName("Given PERMISSION_GROUP 타입 + 일치하는 그룹 - When appliesTo - Then true")
        void appliesTo_permissionGroupType_matchingGroup_returnsTrue() {
            // Given
            String groupCode = "ADMIN_GROUP";
            MenuViewConfig config = MenuViewConfig.forPermissionGroup(
                    createTestMenu(), groupCode, VisibilityAction.SHOW);

            // When & Then
            assertThat(config.appliesTo(groupCode)).isTrue();
        }

        @Test
        @DisplayName("Given PERMISSION_GROUP 타입 + 불일치 그룹 - When appliesTo - Then false")
        void appliesTo_permissionGroupType_differentGroup_returnsFalse() {
            // Given
            MenuViewConfig config = MenuViewConfig.forPermissionGroup(
                    createTestMenu(), "ADMIN_GROUP", VisibilityAction.SHOW);

            // When & Then
            assertThat(config.appliesTo("USER_GROUP")).isFalse();
        }

        @Test
        @DisplayName("Given ORG_POLICY 타입 - When appliesTo(permGroupCode) - Then false")
        void appliesTo_orgPolicyType_alwaysFalse() {
            // Given
            MenuViewConfig config = MenuViewConfig.forOrgPolicy(
                    createTestMenu(), 123L, VisibilityAction.SHOW);

            // When & Then
            assertThat(config.appliesTo("ANY_GROUP")).isFalse();
        }
    }

    @Nested
    @DisplayName("appliesTo 메서드 - 조직 정책")
    class AppliesToOrgPolicyTest {

        @Test
        @DisplayName("Given GLOBAL 타입 - When appliesTo(orgPolicyId) - Then true")
        void appliesTo_globalType_alwaysTrue() {
            // Given
            MenuViewConfig config = MenuViewConfig.forGlobal(createTestMenu(), VisibilityAction.SHOW);

            // When & Then
            assertThat(config.appliesTo(123L)).isTrue();
            assertThat(config.appliesTo((Long) null)).isTrue();
        }

        @Test
        @DisplayName("Given ORG_POLICY 타입 + 일치하는 ID - When appliesTo - Then true")
        void appliesTo_orgPolicyType_matchingId_returnsTrue() {
            // Given
            Long orgPolicyId = 123L;
            MenuViewConfig config = MenuViewConfig.forOrgPolicy(
                    createTestMenu(), orgPolicyId, VisibilityAction.SHOW);

            // When & Then
            assertThat(config.appliesTo(orgPolicyId)).isTrue();
        }

        @Test
        @DisplayName("Given ORG_POLICY 타입 + 불일치 ID - When appliesTo - Then false")
        void appliesTo_orgPolicyType_differentId_returnsFalse() {
            // Given
            MenuViewConfig config = MenuViewConfig.forOrgPolicy(
                    createTestMenu(), 123L, VisibilityAction.SHOW);

            // When & Then
            assertThat(config.appliesTo(456L)).isFalse();
        }

        @Test
        @DisplayName("Given PERMISSION_GROUP 타입 - When appliesTo(orgPolicyId) - Then false")
        void appliesTo_permissionGroupType_alwaysFalse() {
            // Given
            MenuViewConfig config = MenuViewConfig.forPermissionGroup(
                    createTestMenu(), "GROUP", VisibilityAction.SHOW);

            // When & Then
            assertThat(config.appliesTo(123L)).isFalse();
        }
    }

    @Nested
    @DisplayName("기본값 및 Setter")
    class DefaultsAndSettersTest {

        @Test
        @DisplayName("Given 새 설정 - When 생성 - Then 기본값 확인")
        void newConfig_hasDefaultValues() {
            // Given & When
            MenuViewConfig config = MenuViewConfig.forGlobal(createTestMenu(), VisibilityAction.SHOW);

            // Then
            assertThat(config.isActive()).isTrue();
            assertThat(config.getPriority()).isEqualTo(0);
            assertThat(config.getDescription()).isNull();
        }

        @Test
        @DisplayName("Given 설정 - When setter 호출 - Then 값 변경")
        void setters_updateValues() {
            // Given
            MenuViewConfig config = MenuViewConfig.forGlobal(createTestMenu(), VisibilityAction.SHOW);

            // When
            config.setPriority(10);
            config.setDescription("테스트 설명");
            config.setActive(false);
            config.setVisibilityAction(VisibilityAction.HIDE);

            // Then
            assertThat(config.getPriority()).isEqualTo(10);
            assertThat(config.getDescription()).isEqualTo("테스트 설명");
            assertThat(config.isActive()).isFalse();
            assertThat(config.getVisibilityAction()).isEqualTo(VisibilityAction.HIDE);
        }
    }
}
