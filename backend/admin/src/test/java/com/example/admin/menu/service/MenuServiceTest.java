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
import com.example.admin.menu.repository.MenuRepository;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
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
    @DisplayName("createMenu 메서드")
    class CreateMenu {

        @Test
        @DisplayName("Given 새 코드 When 생성하면 Then 메뉴 저장됨")
        void createsNewMenu() {
            given(menuRepository.existsByCode("NEW")).willReturn(false);
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.createMenu("NEW", "새메뉴", "/new", "icon", 1, "설명", null);

            assertThat(result.getCode()).isEqualTo("NEW");
            assertThat(result.getName()).isEqualTo("새메뉴");
            verify(menuRepository).save(any(Menu.class));
        }

        @Test
        @DisplayName("Given 기존 코드 When 생성하면 Then 예외 발생")
        void throwsOnDuplicateCode() {
            given(menuRepository.existsByCode("EXISTING")).willReturn(true);

            assertThatThrownBy(() -> menuService.createMenu("EXISTING", "이름", null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 존재하는 메뉴 코드입니다");
        }

        @Test
        @DisplayName("Given Capabilities When 생성하면 Then Capability 설정됨")
        void createsWithCapabilities() {
            MenuCapability cap = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
            given(menuRepository.existsByCode("CAP")).willReturn(false);
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            Menu result = menuService.createMenu("CAP", "권한메뉴", null, null, null, null, Set.of(cap));

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
}
