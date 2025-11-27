package com.example.admin.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("MenuVisibilityService 테스트")
class MenuVisibilityServiceTest {

    @Mock
    private MenuService menuService;

    @Mock
    private MenuViewConfigRepository viewConfigRepository;

    private MenuVisibilityService visibilityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        visibilityService = new MenuVisibilityService(menuService, viewConfigRepository);
    }

    @Nested
    @DisplayName("determineVisibleMenus 메서드")
    class DetermineVisibleMenus {

        @Test
        @DisplayName("Given 빈 접근가능메뉴 When 호출하면 Then 빈 목록 반환")
        void givenNoAccessibleMenus_returnsEmpty() {
            given(menuService.findAccessibleMenus(any())).willReturn(List.of());

            List<MenuVisibilityService.VisibleMenu> result = visibilityService.determineVisibleMenus(
                    List.of(), "GROUP", 1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 접근가능메뉴 Without ViewConfig When 호출하면 Then 기본 SHOW 적용")
        void givenAccessibleMenusWithoutConfig_returnsWithShow() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu menu = new Menu("MENU1", "메뉴1");
            menu.updateDetails("메뉴1", "/menu1", "icon1", 1, "설명");
            menu.addCapability(cap);

            given(menuService.findAccessibleMenus(any())).willReturn(List.of(menu));
            given(viewConfigRepository.findApplicableConfigsForMenus(anyList(), anyString(), anyLong()))
                    .willReturn(List.of());

            List<MenuVisibilityService.VisibleMenu> result = visibilityService.determineVisibleMenus(
                    List.of(cap), "GROUP", 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("MENU1");
            assertThat(result.get(0).highlighted()).isFalse();
        }

        @Test
        @DisplayName("Given HIDE ViewConfig When 호출하면 Then 메뉴 제외됨")
        void givenHideConfig_excludesMenu() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu menu = new Menu("HIDDEN", "숨김메뉴");
            menu.addCapability(cap);

            MenuViewConfig hideConfig = MenuViewConfig.forGlobal(menu, MenuViewConfig.VisibilityAction.HIDE);

            given(menuService.findAccessibleMenus(any())).willReturn(List.of(menu));
            given(viewConfigRepository.findApplicableConfigsForMenus(anyList(), anyString(), anyLong()))
                    .willReturn(List.of(hideConfig));

            List<MenuVisibilityService.VisibleMenu> result = visibilityService.determineVisibleMenus(
                    List.of(cap), "GROUP", 1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given HIGHLIGHT ViewConfig When 호출하면 Then highlighted=true")
        void givenHighlightConfig_setsHighlighted() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu menu = new Menu("HIGHLIGHT", "강조메뉴");
            menu.updateDetails("강조메뉴", "/highlight", "star", 1, null);
            menu.addCapability(cap);

            MenuViewConfig highlightConfig = MenuViewConfig.forGlobal(menu, MenuViewConfig.VisibilityAction.HIGHLIGHT);

            given(menuService.findAccessibleMenus(any())).willReturn(List.of(menu));
            given(viewConfigRepository.findApplicableConfigsForMenus(anyList(), anyString(), anyLong()))
                    .willReturn(List.of(highlightConfig));

            List<MenuVisibilityService.VisibleMenu> result = visibilityService.determineVisibleMenus(
                    List.of(cap), "GROUP", 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).highlighted()).isTrue();
        }
    }

    @Nested
    @DisplayName("determineMenuVisibility 메서드")
    class DetermineMenuVisibility {

        @Test
        @DisplayName("Given 존재하지 않는 메뉴 When 호출하면 Then empty 반환")
        void givenNonExistentMenu_returnsEmpty() {
            given(menuService.findActiveByCode("MISSING")).willReturn(Optional.empty());

            Optional<MenuVisibilityService.VisibleMenu> result = visibilityService.determineMenuVisibility(
                    "MISSING", List.of(), "GROUP", 1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given Capability 미보유 When 호출하면 Then empty 반환")
        void givenNoCapability_returnsEmpty() {
            MenuCapability requiredCap = new MenuCapability(FeatureCode.DRAFT, ActionCode.CREATE);
            MenuCapability userCap = new MenuCapability(FeatureCode.ORGANIZATION, ActionCode.READ);
            Menu menu = new Menu("MENU", "메뉴");
            menu.addCapability(requiredCap);

            given(menuService.findActiveByCode("MENU")).willReturn(Optional.of(menu));

            Optional<MenuVisibilityService.VisibleMenu> result = visibilityService.determineMenuVisibility(
                    "MENU", List.of(userCap), "GROUP", 1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given null capabilities When 호출하면 Then Capability 없는 메뉴만 visible")
        void givenNullCapabilities_onlyNoRequirementMenusVisible() {
            Menu noReqMenu = new Menu("NO_REQ", "요구없음");
            // 요구 capability가 없는 메뉴

            given(menuService.findActiveByCode("NO_REQ")).willReturn(Optional.of(noReqMenu));
            given(viewConfigRepository.findApplicableConfigs(anyString(), anyString(), anyLong()))
                    .willReturn(List.of());

            Optional<MenuVisibilityService.VisibleMenu> result = visibilityService.determineMenuVisibility(
                    "NO_REQ", null, "GROUP", 1L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Given Capability 보유 When 호출하면 Then VisibleMenu 반환")
        void givenMatchingCapability_returnsVisibleMenu() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu menu = new Menu("ACCESSIBLE", "접근가능");
            menu.updateDetails("접근가능", "/access", "icon", 1, "설명");
            menu.addCapability(cap);

            given(menuService.findActiveByCode("ACCESSIBLE")).willReturn(Optional.of(menu));
            given(viewConfigRepository.findApplicableConfigs(anyString(), anyString(), anyLong()))
                    .willReturn(List.of());

            Optional<MenuVisibilityService.VisibleMenu> result = visibilityService.determineMenuVisibility(
                    "ACCESSIBLE", List.of(cap), "GROUP", 1L);

            assertThat(result).isPresent();
            assertThat(result.get().code()).isEqualTo("ACCESSIBLE");
            assertThat(result.get().path()).isEqualTo("/access");
        }

        @Test
        @DisplayName("Given HIDE config When 호출하면 Then empty 반환")
        void givenHideConfig_returnsEmpty() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu menu = new Menu("HIDDEN", "숨김");
            menu.addCapability(cap);

            MenuViewConfig hideConfig = MenuViewConfig.forGlobal(menu, MenuViewConfig.VisibilityAction.HIDE);

            given(menuService.findActiveByCode("HIDDEN")).willReturn(Optional.of(menu));
            given(viewConfigRepository.findApplicableConfigs(anyString(), anyString(), anyLong()))
                    .willReturn(List.of(hideConfig));

            Optional<MenuVisibilityService.VisibleMenu> result = visibilityService.determineMenuVisibility(
                    "HIDDEN", List.of(cap), "GROUP", 1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("buildVisibleMenuTree 메서드")
    class BuildVisibleMenuTree {

        @Test
        @DisplayName("Given 빈 visibleMenus When 호출하면 Then 빈 트리 반환")
        void givenNoVisibleMenus_returnsEmptyTree() {
            given(menuService.findAccessibleMenus(any())).willReturn(List.of());

            List<MenuVisibilityService.MenuTreeNode> result = visibilityService.buildVisibleMenuTree(
                    List.of(), "GROUP", 1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 단일 메뉴 When 호출하면 Then 단일 노드 트리 반환")
        void givenSingleMenu_returnsSingleNodeTree() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu menu = new Menu("ROOT", "루트");
            menu.updateDetails("루트", "/root", "home", 1, null);
            menu.addCapability(cap);

            given(menuService.findAccessibleMenus(any())).willReturn(List.of(menu));
            given(viewConfigRepository.findApplicableConfigsForMenus(anyList(), anyString(), anyLong()))
                    .willReturn(List.of());

            List<MenuVisibilityService.MenuTreeNode> result = visibilityService.buildVisibleMenuTree(
                    List.of(cap), "GROUP", 1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).menu().code()).isEqualTo("ROOT");
            assertThat(result.get(0).children()).isEmpty();
        }

        @Test
        @DisplayName("Given 부모-자식 메뉴 When 호출하면 Then 계층 트리 반환")
        void givenParentChildMenus_returnsHierarchicalTree() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);

            Menu parent = new Menu("PARENT", "부모");
            parent.updateDetails("부모", "/parent", "folder", 1, null);

            Menu child = new Menu("CHILD", "자식");
            child.updateDetails("자식", "/parent/child", "file", 1, null);
            child.setParent(parent);
            child.addCapability(cap);

            given(menuService.findAccessibleMenus(any())).willReturn(List.of(child));
            given(viewConfigRepository.findApplicableConfigsForMenus(anyList(), anyString(), anyLong()))
                    .willReturn(List.of());
            given(menuService.findActiveByCode("PARENT")).willReturn(Optional.of(parent));

            List<MenuVisibilityService.MenuTreeNode> result = visibilityService.buildVisibleMenuTree(
                    List.of(cap), "GROUP", 1L);

            // 부모가 루트로 포함되어야 함
            assertThat(result).hasSize(1);
            assertThat(result.get(0).menu().code()).isEqualTo("PARENT");
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(0).children().get(0).menu().code()).isEqualTo("CHILD");
        }

        @Test
        @DisplayName("Given 다중 루트 메뉴 When 호출하면 Then sortOrder로 정렬됨")
        void givenMultipleRoots_sortsBySortOrder() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);

            Menu menu1 = new Menu("MENU1", "메뉴1");
            menu1.updateDetails("메뉴1", "/menu1", "icon1", 3, null);
            menu1.addCapability(cap);

            Menu menu2 = new Menu("MENU2", "메뉴2");
            menu2.updateDetails("메뉴2", "/menu2", "icon2", 1, null);
            menu2.addCapability(cap);

            Menu menu3 = new Menu("MENU3", "메뉴3");
            menu3.updateDetails("메뉴3", "/menu3", "icon3", 2, null);
            menu3.addCapability(cap);

            given(menuService.findAccessibleMenus(any())).willReturn(List.of(menu1, menu2, menu3));
            given(viewConfigRepository.findApplicableConfigsForMenus(anyList(), anyString(), anyLong()))
                    .willReturn(List.of());

            List<MenuVisibilityService.MenuTreeNode> result = visibilityService.buildVisibleMenuTree(
                    List.of(cap), "GROUP", 1L);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).menu().sortOrder()).isEqualTo(1);
            assertThat(result.get(1).menu().sortOrder()).isEqualTo(2);
            assertThat(result.get(2).menu().sortOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("Given null sortOrder When 정렬하면 Then 마지막에 배치")
        void givenNullSortOrder_placedAtEnd() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);

            Menu menuWithOrder = new Menu("ORDERED", "정렬됨");
            menuWithOrder.updateDetails("정렬됨", "/ordered", "icon", 1, null);
            menuWithOrder.addCapability(cap);

            Menu menuWithoutOrder = new Menu("UNORDERED", "미정렬");
            menuWithoutOrder.updateDetails("미정렬", "/unordered", "icon", null, null);
            menuWithoutOrder.addCapability(cap);

            given(menuService.findAccessibleMenus(any())).willReturn(List.of(menuWithoutOrder, menuWithOrder));
            given(viewConfigRepository.findApplicableConfigsForMenus(anyList(), anyString(), anyLong()))
                    .willReturn(List.of());

            List<MenuVisibilityService.MenuTreeNode> result = visibilityService.buildVisibleMenuTree(
                    List.of(cap), "GROUP", 1L);

            assertThat(result.get(0).menu().sortOrder()).isEqualTo(1);
            assertThat(result.get(1).menu().sortOrder()).isNull();
        }

        @Test
        @DisplayName("Given 자식이 있는 부모 When 트리 구성하면 Then 자식도 정렬됨")
        void givenChildrenInParent_childrenAreSorted() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);

            Menu parent = new Menu("PARENT", "부모");
            parent.updateDetails("부모", "/parent", "folder", 1, null);

            Menu child1 = new Menu("CHILD1", "자식1");
            child1.updateDetails("자식1", "/parent/child1", "file", 2, null);
            child1.setParent(parent);
            child1.addCapability(cap);

            Menu child2 = new Menu("CHILD2", "자식2");
            child2.updateDetails("자식2", "/parent/child2", "file", 1, null);
            child2.setParent(parent);
            child2.addCapability(cap);

            given(menuService.findAccessibleMenus(any())).willReturn(List.of(child1, child2));
            given(viewConfigRepository.findApplicableConfigsForMenus(anyList(), anyString(), anyLong()))
                    .willReturn(List.of());
            given(menuService.findActiveByCode("PARENT")).willReturn(Optional.of(parent));

            List<MenuVisibilityService.MenuTreeNode> result = visibilityService.buildVisibleMenuTree(
                    List.of(cap), "GROUP", 1L);

            assertThat(result).hasSize(1);
            List<MenuVisibilityService.MenuTreeNode> children = result.get(0).children();
            assertThat(children).hasSize(2);
            assertThat(children.get(0).menu().code()).isEqualTo("CHILD2"); // sortOrder 1
            assertThat(children.get(1).menu().code()).isEqualTo("CHILD1"); // sortOrder 2
        }
    }

    @Nested
    @DisplayName("VisibleMenu record")
    class VisibleMenuRecord {

        @Test
        @DisplayName("Given 모든 필드 When 생성하면 Then 올바른 값 반환")
        void createsWithAllFields() {
            MenuVisibilityService.VisibleMenu vm = new MenuVisibilityService.VisibleMenu(
                    "CODE", "이름", "/path", "icon", 1, "PARENT", "설명", true);

            assertThat(vm.code()).isEqualTo("CODE");
            assertThat(vm.name()).isEqualTo("이름");
            assertThat(vm.path()).isEqualTo("/path");
            assertThat(vm.icon()).isEqualTo("icon");
            assertThat(vm.sortOrder()).isEqualTo(1);
            assertThat(vm.parentCode()).isEqualTo("PARENT");
            assertThat(vm.description()).isEqualTo("설명");
            assertThat(vm.highlighted()).isTrue();
        }
    }

    @Nested
    @DisplayName("MenuTreeNode record")
    class MenuTreeNodeRecord {

        @Test
        @DisplayName("Given 메뉴와 자식 When 생성하면 Then 올바른 구조")
        void createsTreeNode() {
            MenuVisibilityService.VisibleMenu parentVm = new MenuVisibilityService.VisibleMenu(
                    "PARENT", "부모", "/parent", null, 1, null, null, false);
            MenuVisibilityService.VisibleMenu childVm = new MenuVisibilityService.VisibleMenu(
                    "CHILD", "자식", "/child", null, 1, "PARENT", null, false);

            MenuVisibilityService.MenuTreeNode childNode = new MenuVisibilityService.MenuTreeNode(childVm, List.of());
            MenuVisibilityService.MenuTreeNode parentNode = new MenuVisibilityService.MenuTreeNode(parentVm, List.of(childNode));

            assertThat(parentNode.menu().code()).isEqualTo("PARENT");
            assertThat(parentNode.children()).hasSize(1);
            assertThat(parentNode.children().get(0).menu().code()).isEqualTo("CHILD");
        }
    }
}
