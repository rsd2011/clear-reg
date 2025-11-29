package com.example.server.readmodel;

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCapability;
import com.example.admin.menu.service.MenuService;
import com.example.dw.application.readmodel.MenuItem;
import com.example.dw.application.readmodel.MenuItem.MenuCapabilityRef;
import com.example.dw.application.readmodel.MenuReadModel;
import com.example.dw.application.readmodel.MenuReadModelSource;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 메뉴 Read Model 생성기.
 *
 * <p>데이터베이스에서 메뉴 정의를 로드하여 MenuItem 목록을 생성한다.
 * 메뉴는 하나 이상의 Capability(Feature + Action)를 참조할 수 있으며,
 * 이 정보는 사용자 권한 기반 메뉴 필터링에 사용된다.</p>
 */
@Component
public class MenuReadModelSourceImpl implements MenuReadModelSource {

    private final MenuService menuService;
    private final Clock clock;

    public MenuReadModelSourceImpl(MenuService menuService, Clock clock) {
        this.menuService = menuService;
        this.clock = clock;
    }

    @Override
    public MenuReadModel snapshot() {
        List<Menu> menus = menuService.findAllActive();
        List<MenuItem> items = new ArrayList<>();

        for (Menu menu : menus) {
            MenuItem item = toMenuItem(menu);
            items.add(item);
        }

        // sortOrder 기준 정렬
        items.sort(Comparator.comparing(
                item -> item.sortOrder() != null ? item.sortOrder() : Integer.MAX_VALUE));

        return new MenuReadModel(
                UUID.randomUUID().toString(),
                OffsetDateTime.now(clock),
                items);
    }

    /**
     * Menu 엔티티를 MenuItem으로 변환한다.
     */
    private MenuItem toMenuItem(Menu menu) {
        List<MenuCapabilityRef> capabilities = menu.getRequiredCapabilities().stream()
                .map(this::toCapabilityRef)
                .toList();

        // 첫 번째 capability를 기본 featureCode/actionCode로 사용 (하위 호환성)
        String featureCode = capabilities.isEmpty() ? null : capabilities.get(0).feature();
        String actionCode = capabilities.isEmpty() ? null : capabilities.get(0).action();

        return new MenuItem(
                menu.getCode(),
                menu.getName(),
                featureCode,
                actionCode,
                menu.getPath(),
                menu.getIcon(),
                menu.getSortOrder(),
                null,  // parentCode - Menu 엔티티에 parent 관계 없음
                menu.getDescription(),
                capabilities);
    }

    private MenuCapabilityRef toCapabilityRef(MenuCapability cap) {
        return new MenuCapabilityRef(cap.getFeature().name(), cap.getAction().name());
    }
}
