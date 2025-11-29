package com.example.admin.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCapability;
import com.example.admin.menu.repository.MenuRepository;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.PermissionMenu;
import com.example.admin.permission.repository.PermissionMenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("PermissionMenuService 테스트")
class PermissionMenuServiceTest {

    @Mock
    private PermissionMenuRepository permissionMenuRepository;

    @Mock
    private MenuRepository menuRepository;

    private PermissionMenuService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PermissionMenuService(permissionMenuRepository, menuRepository);
    }

    @Nested
    @DisplayName("getMenuTree 메서드")
    class GetMenuTree {

        @Test
        @DisplayName("Given 빈 권한그룹 When getMenuTree 호출하면 Then 빈 목록 반환")
        void returnsEmptyForNoMenus() {
            given(permissionMenuRepository.findByPermissionGroupCode("EMPTY_GROUP"))
                    .willReturn(List.of());

            List<PermissionMenuService.MenuTreeNode> result = service.getMenuTree("EMPTY_GROUP");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 단일 루트 메뉴 When getMenuTree 호출하면 Then 단일 노드 반환")
        void returnsSingleRootNode() {
            Menu menu = new Menu("DASHBOARD", "대시보드");
            menu.updateDetails("대시보드", "/dashboard", "home", 1, null);
            PermissionMenu pm = PermissionMenu.forMenu("ADMIN", menu, null, 1);

            given(permissionMenuRepository.findByPermissionGroupCode("ADMIN"))
                    .willReturn(List.of(pm));

            List<PermissionMenuService.MenuTreeNode> result = service.getMenuTree("ADMIN");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("DASHBOARD");
            assertThat(result.get(0).isCategory()).isFalse();
            assertThat(result.get(0).children()).isEmpty();
        }

        @Test
        @DisplayName("Given 카테고리와 자식 메뉴 When getMenuTree 호출하면 Then 계층 구조 반환")
        void returnsHierarchicalTree() {
            PermissionMenu category = PermissionMenu.forCategory(
                    "ADMIN", "ADMIN_CAT", "관리", "folder", null, 1);

            Menu childMenu = new Menu("USER_MGMT", "사용자관리");
            childMenu.updateDetails("사용자관리", "/users", "people", 1, null);
            PermissionMenu child = PermissionMenu.forMenu("ADMIN", childMenu, category, 1);

            given(permissionMenuRepository.findByPermissionGroupCode("ADMIN"))
                    .willReturn(List.of(category, child));

            List<PermissionMenuService.MenuTreeNode> result = service.getMenuTree("ADMIN");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("ADMIN_CAT");
            assertThat(result.get(0).isCategory()).isTrue();
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(0).children().get(0).code()).isEqualTo("USER_MGMT");
        }
    }

    @Nested
    @DisplayName("getAccessibleMenuTree 메서드")
    class GetAccessibleMenuTree {

        @Test
        @DisplayName("Given Capability 없는 사용자 When 호출하면 Then 카테고리만 있는 빈 트리")
        void returnsEmptyTreeForNoCapabilities() {
            PermissionMenu category = PermissionMenu.forCategory(
                    "ADMIN", "CAT", "카테고리", "icon", null, 1);

            Menu menu = new Menu("MENU", "메뉴");
            menu.addCapability(new MenuCapability(FeatureCode.DRAFT, ActionCode.READ));
            PermissionMenu pm = PermissionMenu.forMenu("ADMIN", menu, category, 1);

            given(permissionMenuRepository.findByPermissionGroupCode("ADMIN"))
                    .willReturn(List.of(category, pm));

            List<PermissionMenuService.MenuTreeNode> result = 
                    service.getAccessibleMenuTree("ADMIN", Set.of());

            // 카테고리의 자식이 모두 접근 불가하면 카테고리도 안 보임
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given Capability 보유 사용자 When 호출하면 Then 접근 가능 메뉴 포함")
        void includesAccessibleMenus() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);

            PermissionMenu category = PermissionMenu.forCategory(
                    "ADMIN", "CAT", "카테고리", "icon", null, 1);

            Menu menu = new Menu("MENU", "메뉴");
            menu.updateDetails("메뉴", "/menu", "icon", 1, null);
            menu.addCapability(cap);
            PermissionMenu pm = PermissionMenu.forMenu("ADMIN", menu, category, 1);

            given(permissionMenuRepository.findByPermissionGroupCode("ADMIN"))
                    .willReturn(List.of(category, pm));

            List<PermissionMenuService.MenuTreeNode> result = 
                    service.getAccessibleMenuTree("ADMIN", Set.of(cap));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("CAT");
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(0).children().get(0).code()).isEqualTo("MENU");
        }

        @Test
        @DisplayName("Given 요구 Capability 없는 메뉴 When 호출하면 Then 모두 접근 가능")
        void noCapabilityMenusAccessibleToAll() {
            Menu menu = new Menu("PUBLIC", "공개메뉴");
            menu.updateDetails("공개메뉴", "/public", "icon", 1, null);
            // 요구 Capability 없음
            PermissionMenu pm = PermissionMenu.forMenu("USER", menu, null, 1);

            given(permissionMenuRepository.findByPermissionGroupCode("USER"))
                    .willReturn(List.of(pm));

            List<PermissionMenuService.MenuTreeNode> result = 
                    service.getAccessibleMenuTree("USER", Set.of());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("PUBLIC");
        }
    }

    @Nested
    @DisplayName("addMenu 메서드")
    class AddMenu {

        @Test
        @DisplayName("Given 유효한 파라미터 When addMenu 호출하면 Then 메뉴 추가")
        void addsMenu() {
            Menu menu = new Menu("NEW_MENU", "새메뉴");
            given(menuRepository.findByCode("NEW_MENU")).willReturn(Optional.of(menu));
            given(permissionMenuRepository.save(any(PermissionMenu.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionMenu result = service.addMenu("ADMIN", "NEW_MENU", null, 1);

            assertThat(result.getMenu()).isEqualTo(menu);
            assertThat(result.getPermissionGroupCode()).isEqualTo("ADMIN");
            verify(permissionMenuRepository).save(any(PermissionMenu.class));
        }

        @Test
        @DisplayName("Given 존재하지 않는 메뉴 When addMenu 호출하면 Then 예외 발생")
        void throwsOnNonExistentMenu() {
            given(menuRepository.findByCode("INVALID")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.addMenu("ADMIN", "INVALID", null, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Menu not found");
        }
    }

    @Nested
    @DisplayName("addCategory 메서드")
    class AddCategory {

        @Test
        @DisplayName("Given 유효한 파라미터 When addCategory 호출하면 Then 카테고리 추가")
        void addsCategory() {
            given(permissionMenuRepository.save(any(PermissionMenu.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionMenu result = service.addCategory(
                    "ADMIN", "NEW_CAT", "새카테고리", "folder", null, 1);

            assertThat(result.getCategoryCode()).isEqualTo("NEW_CAT");
            assertThat(result.getCategoryName()).isEqualTo("새카테고리");
            assertThat(result.isCategory()).isTrue();
            verify(permissionMenuRepository).save(any(PermissionMenu.class));
        }
    }

    @Nested
    @DisplayName("remove 메서드")
    class Remove {

        @Test
        @DisplayName("Given 존재하는 PermissionMenu When remove 호출하면 Then 삭제됨")
        void removesPermissionMenu() {
            UUID id = UUID.randomUUID();

            service.remove("ADMIN", id);

            verify(permissionMenuRepository).deleteById(id);
        }
    }

    @Nested
    @DisplayName("updateDisplayOrder 메서드")
    class UpdateDisplayOrder {

        @Test
        @DisplayName("Given 존재하는 PermissionMenu When updateDisplayOrder 호출하면 Then 순서 변경됨")
        void updatesDisplayOrder() {
            UUID id = UUID.randomUUID();
            Menu menu = new Menu("MENU", "메뉴");
            PermissionMenu pm = PermissionMenu.forMenu("ADMIN", menu, null, 1);
            given(permissionMenuRepository.findById(id)).willReturn(Optional.of(pm));

            service.updateDisplayOrder("ADMIN", id, 99);

            assertThat(pm.getDisplayOrder()).isEqualTo(99);
        }

        @Test
        @DisplayName("Given 존재하지 않는 PermissionMenu When updateDisplayOrder 호출하면 Then 예외 발생")
        void throwsOnNotFound() {
            UUID id = UUID.randomUUID();
            given(permissionMenuRepository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateDisplayOrder("ADMIN", id, 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PermissionMenu not found");
        }
    }

    @Nested
    @DisplayName("updateParent 메서드")
    class UpdateParent {

        @Test
        @DisplayName("Given 존재하는 PermissionMenu When updateParent 호출하면 Then 부모 변경됨")
        void updatesParent() {
            UUID childId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();
            Menu menu = new Menu("CHILD", "자식");
            PermissionMenu child = PermissionMenu.forMenu("ADMIN", menu, null, 1);
            PermissionMenu newParent = PermissionMenu.forCategory(
                    "ADMIN", "PARENT", "부모", "icon", null, 1);

            given(permissionMenuRepository.findById(childId)).willReturn(Optional.of(child));
            given(permissionMenuRepository.findById(parentId)).willReturn(Optional.of(newParent));

            service.updateParent("ADMIN", childId, parentId);

            assertThat(child.getParent()).isEqualTo(newParent);
        }

        @Test
        @DisplayName("Given null parentId When updateParent 호출하면 Then 부모가 null로 설정됨")
        void setsParentToNull() {
            UUID childId = UUID.randomUUID();
            Menu menu = new Menu("CHILD", "자식");
            PermissionMenu parent = PermissionMenu.forCategory(
                    "ADMIN", "OLD_PARENT", "기존부모", "icon", null, 1);
            PermissionMenu child = PermissionMenu.forMenu("ADMIN", menu, parent, 1);

            given(permissionMenuRepository.findById(childId)).willReturn(Optional.of(child));

            service.updateParent("ADMIN", childId, null);

            assertThat(child.getParent()).isNull();
        }

        @Test
        @DisplayName("Given 존재하지 않는 PermissionMenu When updateParent 호출하면 Then 예외 발생")
        void throwsOnNotFound() {
            UUID id = UUID.randomUUID();
            given(permissionMenuRepository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateParent("ADMIN", id, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PermissionMenu not found");
        }
    }

    @Nested
    @DisplayName("removeAllByPermissionGroup 메서드")
    class RemoveAllByPermissionGroup {

        @Test
        @DisplayName("Given 권한그룹 When removeAllByPermissionGroup 호출하면 Then 전체 삭제됨")
        void removesAllPermissionMenus() {
            service.removeAllByPermissionGroup("ADMIN");

            verify(permissionMenuRepository).deleteByPermissionGroupCode("ADMIN");
        }
    }

    @Nested
    @DisplayName("findByMenuCode 메서드")
    class FindByMenuCode {

        @Test
        @DisplayName("Given 존재하는 메뉴코드 When findByMenuCode 호출하면 Then PermissionMenu 반환")
        void findsPermissionMenu() {
            Menu menu = new Menu("DASHBOARD", "대시보드");
            PermissionMenu pm = PermissionMenu.forMenu("ADMIN", menu, null, 1);
            given(permissionMenuRepository.findByPermissionGroupCodeAndMenuCode("ADMIN", "DASHBOARD"))
                    .willReturn(Optional.of(pm));

            Optional<PermissionMenu> result = service.findByMenuCode("ADMIN", "DASHBOARD");

            assertThat(result).isPresent();
            assertThat(result.get().getMenu().getCode()).isEqualTo("DASHBOARD");
        }
    }

    @Nested
    @DisplayName("findByCategoryCode 메서드")
    class FindByCategoryCode {

        @Test
        @DisplayName("Given 존재하는 카테고리코드 When findByCategoryCode 호출하면 Then PermissionMenu 반환")
        void findsPermissionMenu() {
            PermissionMenu pm = PermissionMenu.forCategory(
                    "ADMIN", "SETTINGS", "설정", "icon", null, 1);
            given(permissionMenuRepository.findByPermissionGroupCodeAndCategoryCode("ADMIN", "SETTINGS"))
                    .willReturn(Optional.of(pm));

            Optional<PermissionMenu> result = service.findByCategoryCode("ADMIN", "SETTINGS");

            assertThat(result).isPresent();
            assertThat(result.get().getCategoryCode()).isEqualTo("SETTINGS");
        }
    }

    @Nested
    @DisplayName("addMenu with parent 메서드")
    class AddMenuWithParent {

        @Test
        @DisplayName("Given 부모 ID When addMenu 호출하면 Then 부모가 설정됨")
        void addsMenuWithParent() {
            UUID parentId = UUID.randomUUID();
            Menu menu = new Menu("CHILD_MENU", "자식메뉴");
            PermissionMenu parent = PermissionMenu.forCategory(
                    "ADMIN", "PARENT_CAT", "부모", "icon", null, 1);

            given(menuRepository.findByCode("CHILD_MENU")).willReturn(Optional.of(menu));
            given(permissionMenuRepository.findById(parentId)).willReturn(Optional.of(parent));
            given(permissionMenuRepository.save(any(PermissionMenu.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionMenu result = service.addMenu("ADMIN", "CHILD_MENU", parentId, 1);

            assertThat(result.getParent()).isEqualTo(parent);
        }
    }

    @Nested
    @DisplayName("addCategory with parent 메서드")
    class AddCategoryWithParent {

        @Test
        @DisplayName("Given 부모 ID When addCategory 호출하면 Then 부모가 설정됨")
        void addsCategoryWithParent() {
            UUID parentId = UUID.randomUUID();
            PermissionMenu parent = PermissionMenu.forCategory(
                    "ADMIN", "PARENT_CAT", "부모", "icon", null, 1);

            given(permissionMenuRepository.findById(parentId)).willReturn(Optional.of(parent));
            given(permissionMenuRepository.save(any(PermissionMenu.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionMenu result = service.addCategory(
                    "ADMIN", "CHILD_CAT", "자식카테고리", "folder", parentId, 1);

            assertThat(result.getParent()).isEqualTo(parent);
        }
    }

    @Nested
    @DisplayName("getAccessibleMenuTree 심층 테스트")
    class GetAccessibleMenuTreeDeep {

        @Test
        @DisplayName("Given null userCapabilities When 호출하면 Then 빈 카테고리 제외")
        void handlesNullCapabilities() {
            PermissionMenu category = PermissionMenu.forCategory(
                    "ADMIN", "CAT", "카테고리", "icon", null, 1);

            Menu menu = new Menu("MENU", "메뉴");
            menu.addCapability(new MenuCapability(FeatureCode.DRAFT, ActionCode.READ));
            PermissionMenu pm = PermissionMenu.forMenu("ADMIN", menu, category, 1);

            given(permissionMenuRepository.findByPermissionGroupCode("ADMIN"))
                    .willReturn(List.of(category, pm));

            List<PermissionMenuService.MenuTreeNode> result =
                    service.getAccessibleMenuTree("ADMIN", null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given 다중 Capability 메뉴 When 하나 보유 시 Then 접근 가능")
        void accessibleWithPartialCapabilities() {
            MenuCapability cap1 = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            MenuCapability cap2 = new MenuCapability(FeatureCode.AUDIT_LOG, ActionCode.READ);

            Menu menu = new Menu("MULTI", "다중권한메뉴");
            menu.updateDetails("다중권한메뉴", "/multi", "icon", 1, null);
            menu.addCapability(cap1);
            menu.addCapability(cap2);
            PermissionMenu pm = PermissionMenu.forMenu("ADMIN", menu, null, 1);

            given(permissionMenuRepository.findByPermissionGroupCode("ADMIN"))
                    .willReturn(List.of(pm));

            // cap1만 보유
            List<PermissionMenuService.MenuTreeNode> result =
                    service.getAccessibleMenuTree("ADMIN", Set.of(cap1));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).code()).isEqualTo("MULTI");
        }
    }

    @Nested
    @DisplayName("buildTree 정렬 테스트")
    class BuildTreeSorting {

        @Test
        @DisplayName("Given displayOrder가 null인 항목 When 트리 빌드하면 Then 마지막으로 정렬")
        void sortsNullDisplayOrderLast() {
            Menu menu1 = new Menu("MENU1", "메뉴1");
            menu1.updateDetails("메뉴1", "/menu1", "icon1", 1, null);
            Menu menu2 = new Menu("MENU2", "메뉴2");
            menu2.updateDetails("메뉴2", "/menu2", "icon2", 1, null);
            Menu menu3 = new Menu("MENU3", "메뉴3");
            menu3.updateDetails("메뉴3", "/menu3", "icon3", 1, null);

            PermissionMenu pm1 = PermissionMenu.forMenu("ADMIN", menu1, null, 2);
            PermissionMenu pm2 = PermissionMenu.forMenu("ADMIN", menu2, null, null);
            PermissionMenu pm3 = PermissionMenu.forMenu("ADMIN", menu3, null, 1);

            given(permissionMenuRepository.findByPermissionGroupCode("ADMIN"))
                    .willReturn(List.of(pm1, pm2, pm3));

            List<PermissionMenuService.MenuTreeNode> result = service.getMenuTree("ADMIN");

            assertThat(result).hasSize(3);
            assertThat(result.get(0).code()).isEqualTo("MENU3"); // displayOrder=1
            assertThat(result.get(1).code()).isEqualTo("MENU1"); // displayOrder=2
            assertThat(result.get(2).code()).isEqualTo("MENU2"); // displayOrder=null (마지막)
        }

        @Test
        @DisplayName("Given 모두 null displayOrder When 트리 빌드하면 Then 순서 유지")
        void handlesAllNullDisplayOrder() {
            Menu menu1 = new Menu("MENU1", "메뉴1");
            menu1.updateDetails("메뉴1", "/menu1", "icon1", 1, null);
            Menu menu2 = new Menu("MENU2", "메뉴2");
            menu2.updateDetails("메뉴2", "/menu2", "icon2", 1, null);

            PermissionMenu pm1 = PermissionMenu.forMenu("ADMIN", menu1, null, null);
            PermissionMenu pm2 = PermissionMenu.forMenu("ADMIN", menu2, null, null);

            given(permissionMenuRepository.findByPermissionGroupCode("ADMIN"))
                    .willReturn(List.of(pm1, pm2));

            List<PermissionMenuService.MenuTreeNode> result = service.getMenuTree("ADMIN");

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("MenuTreeNode record")
    class MenuTreeNodeRecord {

        @Test
        @DisplayName("Given MenuTreeNode When withChildren 호출하면 Then 새 인스턴스 반환")
        void withChildrenCreatesNewInstance() {
            PermissionMenuService.MenuTreeNode original = new PermissionMenuService.MenuTreeNode(
                    UUID.randomUUID(), "CODE", "이름", "/path", "icon",
                    1, false, Set.of(), List.of()
            );

            PermissionMenuService.MenuTreeNode child = new PermissionMenuService.MenuTreeNode(
                    UUID.randomUUID(), "CHILD", "자식", "/child", "icon",
                    1, false, Set.of(), List.of()
            );

            PermissionMenuService.MenuTreeNode result = original.withChildren(List.of(child));

            assertThat(result).isNotSameAs(original);
            assertThat(result.code()).isEqualTo("CODE");
            assertThat(result.children()).hasSize(1);
            assertThat(result.children().get(0).code()).isEqualTo("CHILD");
        }
    }
}
