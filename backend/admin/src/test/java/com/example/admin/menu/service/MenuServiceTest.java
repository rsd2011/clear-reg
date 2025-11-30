package com.example.admin.menu.service;

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

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCapability;
import com.example.admin.menu.domain.MenuCode;
import com.example.admin.menu.repository.MenuRepository;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
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

    private MenuService menuService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        menuService = new MenuService(menuRepository);
    }

    @Nested
    @DisplayName("조회 메서드")
    class QueryMethods {

        @Test
        @DisplayName("Given ID When findById 호출하면 Then 메뉴 반환")
        void findById_returnsMenu() {
            UUID id = UUID.randomUUID();
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            given(menuRepository.findById(id)).willReturn(Optional.of(menu));

            Optional<Menu> result = menuService.findById(id);

            assertThat(result).isPresent();
            assertThat(result.get().getCode()).isEqualTo(MenuCode.DASHBOARD);
        }

        @Test
        @DisplayName("Given 코드 When findByCode 호출하면 Then 메뉴 반환")
        void findByCode_returnsMenu() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            given(menuRepository.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.of(menu));

            Optional<Menu> result = menuService.findByCode(MenuCode.DASHBOARD);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("대시보드");
        }

        @Test
        @DisplayName("Given 활성 코드 When findActiveByCode 호출하면 Then 활성 메뉴 반환")
        void findActiveByCode_returnsActiveMenu() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            given(menuRepository.findByCodeAndActiveTrue(MenuCode.DASHBOARD)).willReturn(Optional.of(menu));

            Optional<Menu> result = menuService.findActiveByCode(MenuCode.DASHBOARD);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Given 활성 메뉴 When findAllActive 호출하면 Then 정렬된 목록 반환")
        void findAllActive_returnsSortedList() {
            Menu m1 = new Menu(MenuCode.DASHBOARD, "대시보드");
            Menu m2 = new Menu(MenuCode.DRAFT, "기안");
            given(menuRepository.findByActiveTrueOrderBySortOrderAsc()).willReturn(List.of(m1, m2));

            List<Menu> result = menuService.findAllActive();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given Feature와 Action When findByRequiredCapability 호출하면 Then 해당 Capability 요구 메뉴 반환")
        void findByRequiredCapability_returnsMenusRequiringCapability() {
            Menu menu = new Menu(MenuCode.DRAFT, "기안");
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
            Menu menu = new Menu(MenuCode.DRAFT, "기안");
            menu.addCapability(cap);
            given(menuRepository.findByActiveTrueOrderBySortOrderAsc()).willReturn(List.of(menu));

            List<Menu> result = menuService.findAccessibleMenus(List.of(cap));

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Given Capability 없는 메뉴 When 호출하면 Then 모든 사용자에게 표시")
        void givenMenuWithoutCapabilities_visibleToAll() {
            MenuCapability userCap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            Menu menuNoReq = new Menu(MenuCode.DASHBOARD, "대시보드");
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
            Menu menu = new Menu(MenuCode.ORG_MGMT, "조직 관리");
            menu.addCapability(menuReq);
            given(menuRepository.findByActiveTrueOrderBySortOrderAsc()).willReturn(List.of(menu));

            List<Menu> result = menuService.findAccessibleMenus(List.of(userCap));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createOrUpdateMenu 메서드")
    class CreateOrUpdateMenu {

        @Test
        @DisplayName("Given 새 코드 When 생성하면 Then 메뉴 저장됨")
        void createsNewMenu() {
            given(menuRepository.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.empty());
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.createOrUpdateMenu(
                    MenuCode.DASHBOARD, "대시보드", "custom-icon", 1, "설명", null);

            assertThat(result.getCode()).isEqualTo(MenuCode.DASHBOARD);
            assertThat(result.getName()).isEqualTo("대시보드");
            assertThat(result.getIcon()).isEqualTo("custom-icon");
            verify(menuRepository).save(any(Menu.class));
        }

        @Test
        @DisplayName("Given 기존 메뉴 When 업데이트하면 Then 업데이트됨")
        void updatesExistingMenu() {
            Menu existing = new Menu(MenuCode.DASHBOARD, "기존 이름");
            given(menuRepository.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.of(existing));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.createOrUpdateMenu(
                    MenuCode.DASHBOARD, "새 이름", "new-icon", 10, "새 설명", null);

            assertThat(result.getName()).isEqualTo("새 이름");
            assertThat(result.getIcon()).isEqualTo("new-icon");
            assertThat(result.getSortOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("Given Capabilities When 생성하면 Then Capability 설정됨")
        void createsWithCapabilities() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            given(menuRepository.findByCode(MenuCode.DRAFT)).willReturn(Optional.empty());
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.createOrUpdateMenu(
                    MenuCode.DRAFT, "기안", null, null, null, Set.of(cap));

            assertThat(result.getRequiredCapabilities()).contains(cap);
        }

        @Test
        @DisplayName("Given 기존 메뉴 Capabilities When 업데이트하면 Then Capability 교체됨")
        void replacesCapabilitiesOnUpdate() {
            MenuCapability oldCap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            MenuCapability newCap = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);

            Menu existing = new Menu(MenuCode.DRAFT, "기안");
            existing.addCapability(oldCap);
            given(menuRepository.findByCode(MenuCode.DRAFT)).willReturn(Optional.of(existing));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.createOrUpdateMenu(
                    MenuCode.DRAFT, "기안", null, null, null, Set.of(newCap));

            assertThat(result.getRequiredCapabilities()).containsExactly(newCap);
            assertThat(result.getRequiredCapabilities()).doesNotContain(oldCap);
        }
    }

    @Nested
    @DisplayName("deactivateMenu/activateMenu 메서드")
    class ActivationMethods {

        @Test
        @DisplayName("Given 활성 메뉴 When deactivate 호출하면 Then 비활성화")
        void deactivatesMenu() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            menu.setActive(true);
            given(menuRepository.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.of(menu));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            menuService.deactivateMenu(MenuCode.DASHBOARD);

            assertThat(menu.isActive()).isFalse();
            verify(menuRepository).save(menu);
        }

        @Test
        @DisplayName("Given 비활성 메뉴 When activate 호출하면 Then 활성화")
        void activatesMenu() {
            Menu menu = new Menu(MenuCode.DASHBOARD, "대시보드");
            menu.setActive(false);
            given(menuRepository.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.of(menu));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            menuService.activateMenu(MenuCode.DASHBOARD);

            assertThat(menu.isActive()).isTrue();
        }

        @Test
        @DisplayName("Given 없는 코드 When deactivate 호출하면 Then 예외 발생")
        void throwsOnMissingMenuDeactivate() {
            given(menuRepository.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.deactivateMenu(MenuCode.DASHBOARD))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Given 없는 코드 When activate 호출하면 Then 예외 발생")
        void throwsOnMissingMenuActivate() {
            given(menuRepository.findByCode(MenuCode.DASHBOARD)).willReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.activateMenu(MenuCode.DASHBOARD))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("syncMenusFromEnum 메서드")
    class SyncMenusFromEnum {

        @Test
        @DisplayName("Given 모든 MenuCode 미존재 When sync 호출하면 Then 모두 생성")
        void syncsAllNewMenus() {
            for (MenuCode code : MenuCode.values()) {
                given(menuRepository.existsByCode(code)).willReturn(false);
            }
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            int created = menuService.syncMenusFromEnum();

            assertThat(created).isEqualTo(MenuCode.values().length);
        }

        @Test
        @DisplayName("Given 일부 MenuCode 존재 When sync 호출하면 Then 미존재 것만 생성")
        void syncsOnlyNewMenus() {
            // DASHBOARD만 존재한다고 가정
            for (MenuCode code : MenuCode.values()) {
                given(menuRepository.existsByCode(code)).willReturn(code == MenuCode.DASHBOARD);
            }
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            int created = menuService.syncMenusFromEnum();

            assertThat(created).isEqualTo(MenuCode.values().length - 1);
        }

        @Test
        @DisplayName("Given 모든 MenuCode 존재 When sync 호출하면 Then 0개 생성")
        void syncsNoMenusWhenAllExist() {
            for (MenuCode code : MenuCode.values()) {
                given(menuRepository.existsByCode(code)).willReturn(true);
            }

            int created = menuService.syncMenusFromEnum();

            assertThat(created).isEqualTo(0);
            verify(menuRepository, never()).save(any(Menu.class));
        }
    }
}
