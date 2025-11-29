package com.example.server.readmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCapability;
import com.example.admin.menu.service.MenuService;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.dw.application.readmodel.MenuReadModel;

class MenuReadModelSourceImplTest {

    private final MenuService menuService = Mockito.mock(MenuService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2025-01-02T00:00:00Z"), ZoneOffset.UTC);

    private MenuReadModelSourceImpl source;

    @BeforeEach
    void setUp() {
        source = new MenuReadModelSourceImpl(menuService, clock);
    }

    @Test
    void buildsMenuItemsFromMenuEntities() {
        // Setup Menu entities
        Menu menu1 = new Menu("FILE_READ_MENU", "파일 읽기");
        menu1.updateDetails("파일 읽기", "/file/read", null, 1, null);
        menu1.addCapability(new MenuCapability(FeatureCode.FILE, ActionCode.READ));

        Menu menu2 = new Menu("FILE_UNMASK_MENU", "파일 언마스킹");
        menu2.updateDetails("파일 언마스킹", "/file/unmask", null, 2, null);
        menu2.addCapability(new MenuCapability(FeatureCode.FILE, ActionCode.UNMASK));

        when(menuService.findAllActive()).thenReturn(List.of(menu1, menu2));

        MenuReadModel model = source.snapshot();

        assertThat(model.items()).hasSize(2);
        assertThat(model.items()).anyMatch(item -> "FILE".equals(item.featureCode()) && "READ".equals(item.actionCode()));
        assertThat(model.items()).anyMatch(item -> "FILE".equals(item.featureCode()) && "UNMASK".equals(item.actionCode()));
    }

    @Test
    void sortsByDisplayOrder() {
        Menu menu1 = new Menu("MENU_Z", "Z 메뉴");
        menu1.updateDetails("Z 메뉴", "/z", null, 10, null);
        menu1.addCapability(new MenuCapability(FeatureCode.FILE, ActionCode.READ));

        Menu menu2 = new Menu("MENU_A", "A 메뉴");
        menu2.updateDetails("A 메뉴", "/a", null, 1, null);
        menu2.addCapability(new MenuCapability(FeatureCode.DRAFT, ActionCode.READ));

        when(menuService.findAllActive()).thenReturn(List.of(menu1, menu2));

        MenuReadModel model = source.snapshot();

        assertThat(model.items()).hasSize(2);
        // Sorted by sortOrder ascending
        assertThat(model.items().get(0).code()).isEqualTo("MENU_A");
        assertThat(model.items().get(1).code()).isEqualTo("MENU_Z");
    }

    @Test
    void handlesEmptyMenuList() {
        when(menuService.findAllActive()).thenReturn(List.of());

        MenuReadModel model = source.snapshot();

        assertThat(model.items()).isEmpty();
    }

    @Test
    void handlesMenuWithMultipleCapabilities() {
        Menu menu = new Menu("MULTI_CAP_MENU", "다중 권한 메뉴");
        menu.updateDetails("다중 권한 메뉴", "/multi", null, 1, null);
        menu.replaceCapabilities(Set.of(
                new MenuCapability(FeatureCode.FILE, ActionCode.READ),
                new MenuCapability(FeatureCode.FILE, ActionCode.CREATE)
        ));

        when(menuService.findAllActive()).thenReturn(List.of(menu));

        MenuReadModel model = source.snapshot();

        assertThat(model.items()).hasSize(1);
        var item = model.items().get(0);
        assertThat(item.requiredCapabilities()).hasSize(2);
    }
}
