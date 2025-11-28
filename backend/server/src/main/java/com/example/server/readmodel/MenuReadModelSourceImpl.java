package com.example.server.readmodel;

import com.example.admin.menu.domain.MenuDefinition;
import com.example.admin.menu.service.MenuDefinitionLoader;
import com.example.admin.menu.domain.MenuDefinitions;
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
 * YAML 기반 메뉴 Read Model 생성기.
 *
 * <p>menu-definitions.yml 파일에서 메뉴 정의를 로드하여 MenuItem 목록을 생성한다.
 * 메뉴는 하나 이상의 Capability(Feature + Action)를 참조할 수 있으며,
 * 이 정보는 사용자 권한 기반 메뉴 필터링에 사용된다.</p>
 */
@Component
public class MenuReadModelSourceImpl implements MenuReadModelSource {

    private final MenuDefinitionLoader menuDefinitionLoader;
    private final Clock clock;

    public MenuReadModelSourceImpl(MenuDefinitionLoader menuDefinitionLoader, Clock clock) {
        this.menuDefinitionLoader = menuDefinitionLoader;
        this.clock = clock;
    }

    @Override
    public MenuReadModel snapshot() {
        MenuDefinitions definitions = menuDefinitionLoader.load();
        List<MenuItem> items = new ArrayList<>();

        for (MenuDefinition menu : definitions.getMenus()) {
            flattenMenu(menu, null, items);
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
     * 메뉴 정의를 평탄화하여 MenuItem 목록에 추가한다.
     */
    private void flattenMenu(MenuDefinition menu, String parentCode, List<MenuItem> items) {
        List<MenuCapabilityRef> capabilities = menu.getRequiredCapabilities().stream()
                .map(cap -> new MenuCapabilityRef(cap.getFeature(), cap.getAction()))
                .toList();

        // 첫 번째 capability를 기본 featureCode/actionCode로 사용 (하위 호환성)
        String featureCode = capabilities.isEmpty() ? null : capabilities.get(0).feature();
        String actionCode = capabilities.isEmpty() ? null : capabilities.get(0).action();

        MenuItem item = new MenuItem(
                menu.getCode(),
                menu.getName(),
                featureCode,
                actionCode,
                menu.getPath(),
                menu.getIcon(),
                menu.getSortOrder(),
                parentCode,
                menu.getDescription(),
                capabilities);

        items.add(item);

        // 자식 메뉴 재귀 처리
        for (MenuDefinition child : menu.getChildren()) {
            flattenMenu(child, menu.getCode(), items);
        }
    }
}
