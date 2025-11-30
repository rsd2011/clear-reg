package com.example.admin.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
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
        MenuCode code = MenuCode.DASHBOARD;
        String name = "대시보드";

        // When
        Menu menu = new Menu(code, name);

        // Then
        assertThat(menu.getCode()).isEqualTo(code);
        assertThat(menu.getName()).isEqualTo(name);
        assertThat(menu.isActive()).isTrue();
        assertThat(menu.getRequiredCapabilities()).isEmpty();
    }

    @Test
    @DisplayName("Given MenuCode - When getPath - Then enum의 path 반환")
    void getPath_returnsCodePath() {
        // Given
        Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");

        // When & Then
        assertThat(menu.getPath()).isEqualTo("/dashboard");
    }

    @Test
    @DisplayName("Given MenuCode - When getCodeValue - Then enum 이름 문자열 반환")
    void getCodeValue_returnsEnumName() {
        // Given
        Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");

        // When & Then
        assertThat(menu.getCodeValue()).isEqualTo("DASHBOARD");
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
        assertThatThrownBy(() -> new Menu(MenuCode.DASHBOARD, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Nested
    @DisplayName("updateDetails 메서드")
    class UpdateDetailsTest {

        @Test
        @DisplayName("Given 새 상세정보 - When updateDetails - Then 정보 업데이트")
        void updateDetails_validInput_updatesAllFields() {
            // Given
            Menu menu = new Menu(MenuCode.DASHBOARD, "원래 이름");

            // When
            menu.updateDetails("새 이름", "new-icon", 100, "새 설명");

            // Then
            assertThat(menu.getName()).isEqualTo("새 이름");
            assertThat(menu.getIcon()).isEqualTo("new-icon");
            assertThat(menu.getSortOrder()).isEqualTo(100);
            assertThat(menu.getDescription()).isEqualTo("새 설명");
        }

        @Test
        @DisplayName("Given null icon - When updateDetails - Then getDisplayIcon은 enum 기본값 반환")
        void updateDetails_nullIcon_usesEnumDefault() {
            // Given
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");

            // When
            menu.updateDetails("대시보드", null, 1, null);

            // Then
            assertThat(menu.getIcon()).isNull();
            assertThat(menu.getDisplayIcon()).isEqualTo("home"); // enum의 기본 아이콘
        }

        @Test
        @DisplayName("Given custom icon - When getDisplayIcon - Then custom icon 반환")
        void getDisplayIcon_withCustomIcon_returnsCustom() {
            // Given
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            menu.updateDetails("대시보드", "custom-icon", 1, null);

            // When & Then
            assertThat(menu.getDisplayIcon()).isEqualTo("custom-icon");
        }
    }

    @Nested
    @DisplayName("Capability 관리")
    class CapabilityManagementTest {

        @Test
        @DisplayName("Given Capability 추가 - When addCapability - Then 목록에 포함")
        void addCapability_validCapability_addsToSet() {
            // Given
            Menu menu = new Menu(MenuCode.DRAFT, "기안");
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
            Menu menu = new Menu(MenuCode.DRAFT, "기안");
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
            Menu menu = new Menu(MenuCode.DRAFT, "기안");
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
            Menu menu = new Menu(MenuCode.DRAFT, "기안");
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);
            menu.addCapability(cap);

            // When & Then
            assertThat(menu.requiresCapability(cap)).isTrue();
        }

        @Test
        @DisplayName("Given 미포함 Capability - When requiresCapability - Then false 반환")
        void requiresCapability_notContainedCapability_returnsFalse() {
            // Given
            Menu menu = new Menu(MenuCode.DRAFT, "기안");
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);

            // When & Then
            assertThat(menu.requiresCapability(cap)).isFalse();
        }
    }

    @Nested
    @DisplayName("활성화 상태")
    class ActiveStateTest {

        @Test
        @DisplayName("Given 새 메뉴 - When isActive - Then true (기본값)")
        void isActive_newMenu_defaultTrue() {
            // Given
            Menu menu = new Menu(MenuCode.DASHBOARD, "이름");

            // Then
            assertThat(menu.isActive()).isTrue();
        }

        @Test
        @DisplayName("Given 비활성화 - When setActive(false) - Then isActive false")
        void setActive_false_becomesInactive() {
            // Given
            Menu menu = new Menu(MenuCode.DASHBOARD, "이름");

            // When
            menu.setActive(false);

            // Then
            assertThat(menu.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("MenuCode enum")
    class MenuCodeEnumTest {

        @Test
        @DisplayName("Given path - When fromPath - Then 해당 MenuCode 반환")
        void fromPath_validPath_returnsMenuCode() {
            assertThat(MenuCode.fromPath("/dashboard")).isEqualTo(MenuCode.DASHBOARD);
            assertThat(MenuCode.fromPath("/drafts")).isEqualTo(MenuCode.DRAFT);
        }

        @Test
        @DisplayName("Given 없는 path - When fromPath - Then null 반환")
        void fromPath_invalidPath_returnsNull() {
            assertThat(MenuCode.fromPath("/nonexistent")).isNull();
            assertThat(MenuCode.fromPath(null)).isNull();
        }

        @Test
        @DisplayName("Given 문자열 - When fromString - Then 해당 MenuCode 반환")
        void fromString_validString_returnsMenuCode() {
            assertThat(MenuCode.fromString("DASHBOARD")).isEqualTo(MenuCode.DASHBOARD);
            assertThat(MenuCode.fromString("dashboard")).isEqualTo(MenuCode.DASHBOARD);
        }

        @Test
        @DisplayName("Given 없는 문자열 - When fromString - Then null 반환")
        void fromString_invalidString_returnsNull() {
            assertThat(MenuCode.fromString("INVALID")).isNull();
            assertThat(MenuCode.fromString(null)).isNull();
            assertThat(MenuCode.fromString("")).isNull();
        }
    }
}
