package com.example.admin.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PermissionMenu 테스트.
 *
 * <p>PermissionMenu는 이제 permissionGroupCode 대신 PermissionGroupRoot를 FK로 참조합니다.
 */
@DisplayName("PermissionMenu 테스트")
class PermissionMenuTest {

    private PermissionGroupRoot permissionGroup;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now();
        permissionGroup = PermissionGroupRoot.createWithCode("ADMIN", now);
    }

    @Nested
    @DisplayName("forMenu 팩토리 메서드")
    class ForMenuFactory {

        @Test
        @DisplayName("Given 유효한 파라미터 When forMenu 호출하면 Then 메뉴 타입 생성")
        void createsMenuType() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");

            PermissionMenu pm = PermissionMenu.forMenu(permissionGroup, menu, null, 1);

            assertThat(pm.getPermissionGroupCode()).isEqualTo("ADMIN");
            assertThat(pm.getMenu()).isEqualTo(menu);
            assertThat(pm.isMenu()).isTrue();
            assertThat(pm.isCategory()).isFalse();
            assertThat(pm.isRoot()).isTrue();
            assertThat(pm.getDisplayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given 부모 지정 When forMenu 호출하면 Then 부모 설정됨")
        void setsParent() {
            Menu childMenu = new Menu(MenuCode.USER_MGMT, "사용자관리");

            PermissionMenu parent = PermissionMenu.forCategory(
                    permissionGroup, "ADMIN_CAT", "관리", "folder", null, 1);
            PermissionMenu child = PermissionMenu.forMenu(permissionGroup, childMenu, parent, 1);

            assertThat(child.getParent()).isEqualTo(parent);
            assertThat(child.isRoot()).isFalse();
        }

        @Test
        @DisplayName("Given null permissionGroup When forMenu 호출하면 Then NPE 발생")
        void throwsOnNullPermGroupCode() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "메뉴");

            assertThatThrownBy(() -> PermissionMenu.forMenu(null, menu, null, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("permissionGroup");
        }

        @Test
        @DisplayName("Given null menu When forMenu 호출하면 Then NPE 발생")
        void throwsOnNullMenu() {
            assertThatThrownBy(() -> PermissionMenu.forMenu(permissionGroup, null, null, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("menu");
        }
    }

    @Nested
    @DisplayName("forCategory 팩토리 메서드")
    class ForCategoryFactory {

        @Test
        @DisplayName("Given 유효한 파라미터 When forCategory 호출하면 Then 카테고리 타입 생성")
        void createsCategoryType() {
            PermissionMenu pm = PermissionMenu.forCategory(
                    permissionGroup, "SETTINGS", "설정", "settings", null, 1);

            assertThat(pm.getPermissionGroupCode()).isEqualTo("ADMIN");
            assertThat(pm.getCategoryCode()).isEqualTo("SETTINGS");
            assertThat(pm.getCategoryName()).isEqualTo("설정");
            assertThat(pm.getCategoryIcon()).isEqualTo("settings");
            assertThat(pm.isCategory()).isTrue();
            assertThat(pm.isMenu()).isFalse();
            assertThat(pm.getMenu()).isNull();
        }

        @Test
        @DisplayName("Given null categoryCode When forCategory 호출하면 Then NPE 발생")
        void throwsOnNullCategoryCode() {
            assertThatThrownBy(() -> PermissionMenu.forCategory(
                    permissionGroup, null, "이름", "icon", null, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("categoryCode");
        }

        @Test
        @DisplayName("Given null categoryName When forCategory 호출하면 Then NPE 발생")
        void throwsOnNullCategoryName() {
            assertThatThrownBy(() -> PermissionMenu.forCategory(
                    permissionGroup, "CODE", null, "icon", null, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("categoryName");
        }
    }

    @Nested
    @DisplayName("헬퍼 메서드")
    class HelperMethods {

        @Test
        @DisplayName("Given 메뉴 타입 When getCode 호출하면 Then menu.code 반환")
        void getCodeReturnsMenuCode() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            PermissionMenu pm = PermissionMenu.forMenu(permissionGroup, menu, null, 1);

            assertThat(pm.getCode()).isEqualTo("DASHBOARD");
        }

        @Test
        @DisplayName("Given 카테고리 타입 When getCode 호출하면 Then categoryCode 반환")
        void getCodeReturnsCategoryCode() {
            PermissionMenu pm = PermissionMenu.forCategory(
                    permissionGroup, "SETTINGS", "설정", "icon", null, 1);

            assertThat(pm.getCode()).isEqualTo("SETTINGS");
        }

        @Test
        @DisplayName("Given 메뉴 타입 When getName 호출하면 Then menu.name 반환")
        void getNameReturnsMenuName() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            PermissionMenu pm = PermissionMenu.forMenu(permissionGroup, menu, null, 1);

            assertThat(pm.getName()).isEqualTo("대시보드");
        }

        @Test
        @DisplayName("Given 카테고리 타입 When getName 호출하면 Then categoryName 반환")
        void getNameReturnsCategoryName() {
            PermissionMenu pm = PermissionMenu.forCategory(
                    permissionGroup, "SETTINGS", "설정", "icon", null, 1);

            assertThat(pm.getName()).isEqualTo("설정");
        }

        @Test
        @DisplayName("Given 카테고리 타입 When getPath 호출하면 Then null 반환")
        void getPathReturnsNullForCategory() {
            PermissionMenu pm = PermissionMenu.forCategory(
                    permissionGroup, "SETTINGS", "설정", "icon", null, 1);

            assertThat(pm.getPath()).isNull();
        }

        @Test
        @DisplayName("Given 메뉴 타입 When getPath 호출하면 Then menu.path 반환 (enum에서)")
        void getPathReturnsMenuPath() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            PermissionMenu pm = PermissionMenu.forMenu(permissionGroup, menu, null, 1);

            assertThat(pm.getPath()).isEqualTo("/dashboard"); // MenuCode.DASHBOARD의 path
        }
    }

    @Nested
    @DisplayName("Mutator 메서드")
    class Mutators {

        @Test
        @DisplayName("Given PermissionMenu When setParent 호출하면 Then 부모 변경됨")
        void setParentUpdatesParent() {
            Menu menu = new Menu(MenuCode.DRAFT, "기안");
            PermissionMenu child = PermissionMenu.forMenu(permissionGroup, menu, null, 1);
            PermissionMenu newParent = PermissionMenu.forCategory(
                    permissionGroup, "NEW_CAT", "새카테고리", "folder", null, 1);

            child.setParent(newParent);

            assertThat(child.getParent()).isEqualTo(newParent);
            assertThat(child.isRoot()).isFalse();
        }

        @Test
        @DisplayName("Given PermissionMenu When setDisplayOrder 호출하면 Then 순서 변경됨")
        void setDisplayOrderUpdatesOrder() {
            PermissionMenu pm = PermissionMenu.forCategory(
                    permissionGroup, "CAT", "카테고리", "icon", null, 1);

            pm.setDisplayOrder(99);

            assertThat(pm.getDisplayOrder()).isEqualTo(99);
        }
    }

    @Nested
    @DisplayName("toString 메서드")
    class ToStringMethod {

        @Test
        @DisplayName("Given 메뉴 타입 When toString 호출하면 Then 메뉴 정보 포함")
        void toStringForMenuType() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            PermissionMenu pm = PermissionMenu.forMenu(permissionGroup, menu, null, 1);

            String result = pm.toString();

            assertThat(result).contains("permissionGroupCode='ADMIN'");
            assertThat(result).contains("menu=DASHBOARD");
            assertThat(result).contains("parent=null");
            assertThat(result).contains("displayOrder=1");
        }

        @Test
        @DisplayName("Given 카테고리 타입 When toString 호출하면 Then 카테고리 정보 포함")
        void toStringForCategoryType() {
            PermissionMenu pm = PermissionMenu.forCategory(
                    permissionGroup, "SETTINGS", "설정", "icon", null, 1);

            String result = pm.toString();

            assertThat(result).contains("permissionGroupCode='ADMIN'");
            assertThat(result).contains("category=SETTINGS");
            assertThat(result).contains("parent=null");
        }

        @Test
        @DisplayName("Given 부모가 있는 메뉴 When toString 호출하면 Then 부모 코드 포함")
        void toStringWithParent() {
            PermissionMenu parent = PermissionMenu.forCategory(
                    permissionGroup, "PARENT_CAT", "부모", "folder", null, 1);
            Menu menu = new Menu(MenuCode.DRAFT, "기안");
            PermissionMenu child = PermissionMenu.forMenu(permissionGroup, menu, parent, 2);

            String result = child.toString();

            assertThat(result).contains("parent=PARENT_CAT");
        }
    }

    @Nested
    @DisplayName("아이콘 관련 메서드")
    class IconMethods {

        @Test
        @DisplayName("Given 메뉴 타입 (DB 아이콘 설정) When getIcon 호출하면 Then 설정된 아이콘 반환")
        void getIconReturnsMenuIcon() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            menu.updateDetails("대시보드", "home_icon", 1, null);
            PermissionMenu pm = PermissionMenu.forMenu(permissionGroup, menu, null, 1);

            assertThat(pm.getIcon()).isEqualTo("home_icon");
        }

        @Test
        @DisplayName("Given 메뉴 타입 (DB 아이콘 미설정) When getIcon 호출하면 Then enum 기본 아이콘 반환")
        void getIconReturnsDefaultIconWhenNotSet() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            PermissionMenu pm = PermissionMenu.forMenu(permissionGroup, menu, null, 1);

            // MenuCode.DASHBOARD의 defaultIcon은 "home"
            assertThat(pm.getIcon()).isEqualTo("home");
        }

        @Test
        @DisplayName("Given 카테고리 타입 When getIcon 호출하면 Then categoryIcon 반환")
        void getIconReturnsCategoryIcon() {
            PermissionMenu pm = PermissionMenu.forCategory(
                    permissionGroup, "SETTINGS", "설정", "settings_icon", null, 1);

            assertThat(pm.getIcon()).isEqualTo("settings_icon");
        }
    }

    @Nested
    @DisplayName("updateCategory 메서드")
    class UpdateCategory {

        @Test
        @DisplayName("Given 카테고리 타입 When updateCategory 호출하면 Then 정보 업데이트")
        void updatesCategory() {
            PermissionMenu pm = PermissionMenu.forCategory(
                    permissionGroup, "SETTINGS", "설정", "icon", null, 1);

            pm.updateCategory("시스템 설정", "system");

            assertThat(pm.getCategoryName()).isEqualTo("시스템 설정");
            assertThat(pm.getCategoryIcon()).isEqualTo("system");
        }

        @Test
        @DisplayName("Given 메뉴 타입 When updateCategory 호출하면 Then IllegalStateException 발생")
        void throwsOnMenuType() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            PermissionMenu pm = PermissionMenu.forMenu(permissionGroup, menu, null, 1);

            assertThatThrownBy(() -> pm.updateCategory("새이름", "icon"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("menu type");
        }
    }
}
