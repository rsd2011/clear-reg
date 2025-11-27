package com.example.admin.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("MenuService 테스트")
class MenuServiceTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuDefinitionLoader menuDefinitionLoader;

    private MenuService menuService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        menuService = new MenuService(menuRepository, menuDefinitionLoader);
    }

    @Nested
    @DisplayName("조회 메서드")
    class QueryMethods {

        @Test
        @DisplayName("Given ID When findById 호출하면 Then 메뉴 반환")
        void findById_returnsMenu() {
            UUID id = UUID.randomUUID();
            Menu menu = new Menu("TEST", "테스트");
            given(menuRepository.findById(id)).willReturn(Optional.of(menu));

            Optional<Menu> result = menuService.findById(id);

            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo("TEST");
        }

        @Test
        @DisplayName("Given 코드 When findByCode 호출하면 Then 메뉴 반환")
        void findByCode_returnsMenu() {
            Menu menu = new Menu("DASHBOARD", "대시보드");
            given(menuRepository.findByCode("DASHBOARD")).willReturn(Optional.of(menu));

            Optional<Menu> result = menuService.findByCode("DASHBOARD");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("대시보드");
        }

        @Test
        @DisplayName("Given 활성 코드 When findActiveByCode 호출하면 Then 활성 메뉴 반환")
        void findActiveByCode_returnsActiveMenu() {
            Menu menu = new Menu("ACTIVE", "활성메뉴");
            given(menuRepository.findByCodeAndActiveTrue("ACTIVE")).willReturn(Optional.of(menu));

            Optional<Menu> result = menuService.findActiveByCode("ACTIVE");

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Given 활성 메뉴 When findAllActive 호출하면 Then 정렬된 목록 반환")
        void findAllActive_returnsSortedList() {
            Menu m1 = new Menu("M1", "메뉴1");
            Menu m2 = new Menu("M2", "메뉴2");
            given(menuRepository.findByActiveTrueOrderBySortOrderAsc()).willReturn(List.of(m1, m2));

            List<Menu> result = menuService.findAllActive();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given 루트 메뉴 When findRootMenus 호출하면 Then 루트 메뉴 반환")
        void findRootMenus_returnsRootMenus() {
            Menu root = new Menu("ROOT", "루트");
            given(menuRepository.findByParentIsNullAndActiveTrueOrderBySortOrderAsc()).willReturn(List.of(root));

            List<Menu> result = menuService.findRootMenus();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("ROOT");
        }

        @Test
        @DisplayName("Given 부모코드 When findChildrenByParentCode 호출하면 Then 자식 메뉴 반환")
        void findChildrenByParentCode_returnsChildren() {
            Menu child = new Menu("CHILD", "자식");
            given(menuRepository.findByParent_CodeAndActiveTrueOrderBySortOrderAsc("PARENT")).willReturn(List.of(child));

            List<Menu> result = menuService.findChildrenByParentCode("PARENT");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Given Feature와 Action When findByRequiredCapability 호출하면 Then 해당 Capability 요구 메뉴 반환")
        void findByRequiredCapability_returnsMenusRequiringCapability() {
            Menu menu = new Menu("CAP_MENU", "권한메뉴");
            given(menuRepository.findByRequiredCapability(FeatureCode.DRAFT, ActionCode.READ)).willReturn(List.of(menu));

            List<Menu> result = menuService.findByRequiredCapability(FeatureCode.DRAFT, ActionCode.READ);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findAccessibleMenus 메서드")
    class FindAccessibleMenus {

        @Test
        @DisplayName("Given null capabilities When 호출하면 Then 빈 목록 반환")
        void givenNullCapabilities_returnsEmptyList() {
            List<Menu> result = menuService.findAccessibleMenus(null);

            assertThat(result).isEmpty();
            verify(menuRepository, never()).findByActiveTrueOrderBySortOrderAsc();
        }

        @Test
        @DisplayName("Given 빈 capabilities When 호출하면 Then 빈 목록 반환")
        void givenEmptyCapabilities_returnsEmptyList() {
            List<Menu> result = menuService.findAccessibleMenus(List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given Capability 보유 When 호출하면 Then 접근 가능 메뉴 반환")
        void givenCapabilities_returnsAccessibleMenus() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu menu = new Menu("MENU", "메뉴");
            menu.addCapability(cap);
            given(menuRepository.findByActiveTrueOrderBySortOrderAsc()).willReturn(List.of(menu));

            List<Menu> result = menuService.findAccessibleMenus(List.of(cap));

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Given Capability 없는 메뉴 When 호출하면 Then 모든 사용자에게 표시")
        void givenMenuWithoutCapabilities_visibleToAll() {
            MenuCapability userCap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu menuNoReq = new Menu("NO_REQ", "요구없음");
            // 요구하는 capability가 없음
            given(menuRepository.findByActiveTrueOrderBySortOrderAsc()).willReturn(List.of(menuNoReq));

            List<Menu> result = menuService.findAccessibleMenus(List.of(userCap));

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Given 사용자가 미보유한 Capability 요구 메뉴 When 호출하면 Then 해당 메뉴 제외")
        void givenUnmatchedCapability_excludesMenu() {
            MenuCapability userCap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            MenuCapability menuReq = new MenuCapability(FeatureCode.ORGANIZATION, ActionCode.CREATE);
            Menu menu = new Menu("RESTRICTED", "제한됨");
            menu.addCapability(menuReq);
            given(menuRepository.findByActiveTrueOrderBySortOrderAsc()).willReturn(List.of(menu));

            List<Menu> result = menuService.findAccessibleMenus(List.of(userCap));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("buildAccessibleMenuTree 메서드")
    class BuildAccessibleMenuTree {

        @Test
        @DisplayName("Given 계층구조 메뉴 When 호출하면 Then 부모 포함 트리 반환")
        void buildsTreeWithParents() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu root = new Menu("ROOT", "루트");
            Menu child = new Menu("CHILD", "자식");
            child.setParent(root);
            child.addCapability(cap);

            given(menuRepository.findByActiveTrueOrderBySortOrderAsc()).willReturn(List.of(root, child));
            given(menuRepository.findByParentIsNullAndActiveTrueOrderBySortOrderAsc()).willReturn(List.of(root));

            List<Menu> result = menuService.buildAccessibleMenuTree(List.of(cap));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("ROOT");
        }
    }

    @Nested
    @DisplayName("createMenu 메서드")
    class CreateMenu {

        @Test
        @DisplayName("Given 새 코드 When 생성하면 Then 메뉴 저장됨")
        void createsNewMenu() {
            given(menuRepository.existsByCode("NEW")).willReturn(false);
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.createMenu("NEW", "새메뉴", "/new", "icon", 1, "설명", null, null);

            assertThat(result.getCode()).isEqualTo("NEW");
            assertThat(result.getName()).isEqualTo("새메뉴");
            verify(menuRepository).save(any(Menu.class));
        }

        @Test
        @DisplayName("Given 기존 코드 When 생성하면 Then 예외 발생")
        void throwsOnDuplicateCode() {
            given(menuRepository.existsByCode("EXISTING")).willReturn(true);

            assertThatThrownBy(() -> menuService.createMenu("EXISTING", "이름", null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 존재하는 메뉴 코드입니다");
        }

        @Test
        @DisplayName("Given 부모코드 When 생성하면 Then 부모 연결됨")
        void createsWithParent() {
            Menu parent = new Menu("PARENT", "부모");
            given(menuRepository.existsByCode("CHILD")).willReturn(false);
            given(menuRepository.findByCode("PARENT")).willReturn(Optional.of(parent));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.createMenu("CHILD", "자식", null, null, null, null, "PARENT", null);

            assertThat(result.getParent()).isEqualTo(parent);
        }

        @Test
        @DisplayName("Given 없는 부모코드 When 생성하면 Then 예외 발생")
        void throwsOnMissingParent() {
            given(menuRepository.existsByCode("CHILD")).willReturn(false);
            given(menuRepository.findByCode("MISSING")).willReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.createMenu("CHILD", "자식", null, null, null, null, "MISSING", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("부모 메뉴를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Given Capabilities When 생성하면 Then Capability 설정됨")
        void createsWithCapabilities() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            given(menuRepository.existsByCode("CAP")).willReturn(false);
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.createMenu("CAP", "권한메뉴", null, null, null, null, null, Set.of(cap));

            assertThat(result.getRequiredCapabilities()).contains(cap);
        }
    }

    @Nested
    @DisplayName("updateMenu 메서드")
    class UpdateMenu {

        @Test
        @DisplayName("Given 존재하는 코드 When 수정하면 Then 업데이트됨")
        void updatesExistingMenu() {
            Menu existing = new Menu("EXISTING", "기존");
            given(menuRepository.findByCode("EXISTING")).willReturn(Optional.of(existing));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.updateMenu("EXISTING", "수정됨", "/updated", "newIcon", 10, "설명", null);

            assertThat(result.getName()).isEqualTo("수정됨");
            assertThat(result.getPath()).isEqualTo("/updated");
        }

        @Test
        @DisplayName("Given 없는 코드 When 수정하면 Then 예외 발생")
        void throwsOnMissingMenu() {
            given(menuRepository.findByCode("MISSING")).willReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.updateMenu("MISSING", "이름", null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("메뉴를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("deactivateMenu/activateMenu 메서드")
    class ActivationMethods {

        @Test
        @DisplayName("Given 활성 메뉴 When deactivate 호출하면 Then 비활성화")
        void deactivatesMenu() {
            Menu menu = new Menu("ACTIVE", "활성");
            menu.setActive(true);
            given(menuRepository.findByCode("ACTIVE")).willReturn(Optional.of(menu));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            menuService.deactivateMenu("ACTIVE");

            assertThat(menu.isActive()).isFalse();
            verify(menuRepository).save(menu);
        }

        @Test
        @DisplayName("Given 비활성 메뉴 When activate 호출하면 Then 활성화")
        void activatesMenu() {
            Menu menu = new Menu("INACTIVE", "비활성");
            menu.setActive(false);
            given(menuRepository.findByCode("INACTIVE")).willReturn(Optional.of(menu));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            menuService.activateMenu("INACTIVE");

            assertThat(menu.isActive()).isTrue();
        }

        @Test
        @DisplayName("Given 없는 코드 When deactivate 호출하면 Then 예외 발생")
        void throwsOnMissingMenuDeactivate() {
            given(menuRepository.findByCode("MISSING")).willReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.deactivateMenu("MISSING"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("moveMenu 메서드")
    class MoveMenu {

        @Test
        @DisplayName("Given null 부모 When 이동하면 Then 루트로 이동")
        void movesToRoot() {
            Menu parent = new Menu("PARENT", "부모");
            Menu menu = new Menu("MENU", "메뉴");
            menu.setParent(parent);
            given(menuRepository.findByCode("MENU")).willReturn(Optional.of(menu));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            menuService.moveMenu("MENU", null);

            assertThat(menu.getParent()).isNull();
        }

        @Test
        @DisplayName("Given 자기 자신 When 이동하면 Then 예외 발생")
        void throwsOnSelfParent() {
            Menu menu = new Menu("SELF", "자기자신");
            given(menuRepository.findByCode("SELF")).willReturn(Optional.of(menu));

            assertThatThrownBy(() -> menuService.moveMenu("SELF", "SELF"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("자기 자신의 하위로 이동할 수 없습니다");
        }

        @Test
        @DisplayName("Given 없는 부모 When 이동하면 Then 예외 발생")
        void throwsOnMissingParent() {
            Menu menu = new Menu("MENU", "메뉴");
            given(menuRepository.findByCode("MENU")).willReturn(Optional.of(menu));
            given(menuRepository.findByCode("MISSING")).willReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.moveMenu("MENU", "MISSING"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("부모 메뉴를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Given 순환 참조 When 이동하면 Then 예외 발생")
        void throwsOnCircularReference() {
            Menu grandparent = new Menu("GRANDPARENT", "조부모");
            Menu parent = new Menu("PARENT", "부모");
            Menu child = new Menu("CHILD", "자식");
            parent.setParent(grandparent);
            child.setParent(parent);

            given(menuRepository.findByCode("GRANDPARENT")).willReturn(Optional.of(grandparent));
            given(menuRepository.findByCode("CHILD")).willReturn(Optional.of(child));

            assertThatThrownBy(() -> menuService.moveMenu("GRANDPARENT", "CHILD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("순환 참조");
        }

        @Test
        @DisplayName("Given 유효한 부모 When 이동하면 Then 부모 변경됨")
        void movesToValidParent() {
            Menu menu = new Menu("MENU", "메뉴");
            Menu newParent = new Menu("NEW_PARENT", "새부모");
            given(menuRepository.findByCode("MENU")).willReturn(Optional.of(menu));
            given(menuRepository.findByCode("NEW_PARENT")).willReturn(Optional.of(newParent));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            menuService.moveMenu("MENU", "NEW_PARENT");

            assertThat(menu.getParent()).isEqualTo(newParent);
        }
    }

    @Nested
    @DisplayName("syncFromYaml 메서드")
    class SyncFromYaml {

        @Test
        @DisplayName("Given YAML 정의 When sync 호출하면 Then 메뉴 동기화됨")
        void syncsFromYaml() {
            MenuDefinition def = new MenuDefinition();
            def.setCode("YAML_MENU");
            def.setName("YAML메뉴");
            def.setPath("/yaml");
            def.setSortOrder(1);

            given(menuDefinitionLoader.loadFlattened()).willReturn(List.of(def));
            given(menuRepository.findByCode("YAML_MENU")).willReturn(Optional.empty());
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            menuService.syncFromYaml();

            verify(menuRepository).save(any(Menu.class));
        }

        @Test
        @DisplayName("Given 기존 메뉴 When sync 호출하면 Then 업데이트됨")
        void updatesExistingOnSync() {
            MenuDefinition def = new MenuDefinition();
            def.setCode("EXISTING");
            def.setName("업데이트됨");
            def.setPath("/updated");

            Menu existing = new Menu("EXISTING", "기존");
            given(menuDefinitionLoader.loadFlattened()).willReturn(List.of(def));
            given(menuRepository.findByCode("EXISTING")).willReturn(Optional.of(existing));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            menuService.syncFromYaml();

            assertThat(existing.getName()).isEqualTo("업데이트됨");
        }

        @Test
        @DisplayName("Given 유효한 Capability When sync 호출하면 Then Capability 설정됨")
        void syncsWithCapabilities() {
            MenuDefinition.CapabilityRef capRef = new MenuDefinition.CapabilityRef();
            capRef.setFeature("DRAFT");
            capRef.setAction("READ");

            MenuDefinition def = new MenuDefinition();
            def.setCode("WITH_CAP");
            def.setName("권한있는메뉴");
            def.setRequiredCapabilities(List.of(capRef));

            given(menuDefinitionLoader.loadFlattened()).willReturn(List.of(def));
            given(menuRepository.findByCode("WITH_CAP")).willReturn(Optional.empty());
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> {
                Menu m = inv.getArgument(0);
                assertThat(m.getRequiredCapabilities()).isNotEmpty();
                return m;
            });

            menuService.syncFromYaml();
        }

        @Test
        @DisplayName("Given 유효하지 않은 Capability When sync 호출하면 Then 경고 로그 후 스킵")
        void skipsInvalidCapability() {
            MenuDefinition.CapabilityRef invalidCap = new MenuDefinition.CapabilityRef();
            invalidCap.setFeature("INVALID_FEATURE");
            invalidCap.setAction("INVALID_ACTION");

            MenuDefinition def = new MenuDefinition();
            def.setCode("INVALID_CAP");
            def.setName("유효하지않은권한");
            def.setRequiredCapabilities(List.of(invalidCap));

            given(menuDefinitionLoader.loadFlattened()).willReturn(List.of(def));
            given(menuRepository.findByCode("INVALID_CAP")).willReturn(Optional.empty());
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> {
                Menu m = inv.getArgument(0);
                assertThat(m.getRequiredCapabilities()).isEmpty();
                return m;
            });

            menuService.syncFromYaml();
        }

        @Test
        @DisplayName("Given 자식 메뉴 정의 When sync 호출하면 Then 자식도 동기화됨")
        void syncsChildrenMenus() {
            MenuDefinition child = new MenuDefinition();
            child.setCode("CHILD");
            child.setName("자식");

            MenuDefinition parent = new MenuDefinition();
            parent.setCode("PARENT");
            parent.setName("부모");
            parent.setChildren(List.of(child));

            given(menuDefinitionLoader.loadFlattened()).willReturn(List.of(parent));
            given(menuRepository.findByCode("PARENT")).willReturn(Optional.empty());
            given(menuRepository.findByCode("CHILD")).willReturn(Optional.empty());
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            menuService.syncFromYaml();

            // parent와 child 두 번 save 호출
            verify(menuRepository, org.mockito.Mockito.times(2)).save(any(Menu.class));
        }
    }
}
