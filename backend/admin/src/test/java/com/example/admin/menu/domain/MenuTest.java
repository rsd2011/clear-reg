package com.example.admin.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Menu 엔티티 단위 테스트.
 */
class MenuTest {

    @Test
    @DisplayName("Given 유효한 코드/이름 - When Menu 생성 - Then 정상 생성")
    void constructor_validCodeAndName_createsMenu() {
        // Given
        String code = "DRAFT_MANAGEMENT";
        String name = "기안 관리";

        // When
        Menu menu = new Menu(code, name);

        // Then
        assertThat(menu.getCode()).isEqualTo(code);
        assertThat(menu.getName()).isEqualTo(name);
        assertThat(menu.isActive()).isTrue();
        assertThat(menu.getParent()).isNull();
        assertThat(menu.getChildren()).isEmpty();
        assertThat(menu.getRequiredCapabilities()).isEmpty();
    }

    @Test
    @DisplayName("Given null 코드 - When Menu 생성 - Then NullPointerException")
    void constructor_nullCode_throwsException() {
        assertThatThrownBy(() -> new Menu(null, "이름"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Given null 이름 - When Menu 생성 - Then NullPointerException")
    void constructor_nullName_throwsException() {
        assertThatThrownBy(() -> new Menu("CODE", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Nested
    @DisplayName("updateDetails 메서드")
    class UpdateDetailsTest {

        @Test
        @DisplayName("Given 새 상세정보 - When updateDetails - Then 정보 업데이트")
        void updateDetails_validInput_updatesAllFields() {
            // Given
            Menu menu = new Menu("CODE", "원래 이름");

            // When
            menu.updateDetails("새 이름", "/new/path", "new-icon", 100, "새 설명");

            // Then
            assertThat(menu.getName()).isEqualTo("새 이름");
            assertThat(menu.getPath()).isEqualTo("/new/path");
            assertThat(menu.getIcon()).isEqualTo("new-icon");
            assertThat(menu.getSortOrder()).isEqualTo(100);
            assertThat(menu.getDescription()).isEqualTo("새 설명");
        }
    }

    @Nested
    @DisplayName("Capability 관리")
    class CapabilityManagementTest {

        @Test
        @DisplayName("Given Capability 추가 - When addCapability - Then 목록에 포함")
        void addCapability_validCapability_addsToSet() {
            // Given
            Menu menu = new Menu("DRAFT", "기안");
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);

            // When
            menu.addCapability(cap);

            // Then
            assertThat(menu.getRequiredCapabilities()).contains(cap);
        }

        @Test
        @DisplayName("Given Capability 제거 - When removeCapability - Then 목록에서 제외")
        void removeCapability_existingCapability_removesFromSet() {
            // Given
            Menu menu = new Menu("DRAFT", "기안");
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);
            menu.addCapability(cap);

            // When
            menu.removeCapability(cap);

            // Then
            assertThat(menu.getRequiredCapabilities()).doesNotContain(cap);
        }

        @Test
        @DisplayName("Given 새 Capability 세트 - When replaceCapabilities - Then 전체 교체")
        void replaceCapabilities_newSet_replacesAll() {
            // Given
            Menu menu = new Menu("DRAFT", "기안");
            menu.addCapability(new MenuCapability(FeatureCode.DRAFT, ActionCode.READ));

            Set<MenuCapability> newCaps = Set.of(
                    new MenuCapability(FeatureCode.APPROVAL, ActionCode.APPROVAL_REVIEW),
                    new MenuCapability(FeatureCode.APPROVAL, ActionCode.READ)
            );

            // When
            menu.replaceCapabilities(newCaps);

            // Then
            assertThat(menu.getRequiredCapabilities()).hasSize(2);
            assertThat(menu.getRequiredCapabilities()).containsAll(newCaps);
        }

        @Test
        @DisplayName("Given 포함된 Capability - When requiresCapability - Then true 반환")
        void requiresCapability_containedCapability_returnsTrue() {
            // Given
            Menu menu = new Menu("DRAFT", "기안");
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);
            menu.addCapability(cap);

            // When & Then
            assertThat(menu.requiresCapability(cap)).isTrue();
        }

        @Test
        @DisplayName("Given 미포함 Capability - When requiresCapability - Then false 반환")
        void requiresCapability_notContainedCapability_returnsFalse() {
            // Given
            Menu menu = new Menu("DRAFT", "기안");
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);

            // When & Then
            assertThat(menu.requiresCapability(cap)).isFalse();
        }
    }

    @Nested
    @DisplayName("부모-자식 관계")
    class ParentChildRelationshipTest {

        @Test
        @DisplayName("Given 부모 설정 - When getParentCode - Then 부모 코드 반환")
        void setParent_validParent_returnsParentCode() {
            // Given
            Menu parent = new Menu("ADMIN", "관리");
            Menu child = new Menu("ADMIN_POLICY", "정책 관리");

            // When
            child.setParent(parent);

            // Then
            assertThat(child.getParent()).isEqualTo(parent);
            assertThat(child.getParentCode()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Given 부모 없음 - When getParentCode - Then null 반환")
        void getParentCode_noParent_returnsNull() {
            // Given
            Menu menu = new Menu("ROOT", "루트");

            // When & Then
            assertThat(menu.getParentCode()).isNull();
        }

        @Test
        @DisplayName("Given 3단계 계층 - When getFullPath - Then 전체 경로 반환")
        void getFullPath_threeLevel_returnsFullPath() {
            // Given
            Menu root = new Menu("ADMIN", "관리");
            Menu middle = new Menu("ADMIN_SECURITY", "보안");
            Menu leaf = new Menu("ADMIN_SECURITY_POLICY", "보안 정책");

            middle.setParent(root);
            leaf.setParent(middle);

            // When
            List<String> fullPath = leaf.getFullPath();

            // Then
            assertThat(fullPath).containsExactly("ADMIN", "ADMIN_SECURITY", "ADMIN_SECURITY_POLICY");
        }
    }

    @Nested
    @DisplayName("활성화 상태")
    class ActiveStateTest {

        @Test
        @DisplayName("Given 새 메뉴 - When isActive - Then true (기본값)")
        void isActive_newMenu_defaultTrue() {
            // Given
            Menu menu = new Menu("CODE", "이름");

            // Then
            assertThat(menu.isActive()).isTrue();
        }

        @Test
        @DisplayName("Given 비활성화 - When setActive(false) - Then isActive false")
        void setActive_false_becomesInactive() {
            // Given
            Menu menu = new Menu("CODE", "이름");

            // When
            menu.setActive(false);

            // Then
            assertThat(menu.isActive()).isFalse();
        }
    }
}
