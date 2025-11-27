package com.example.dw.application.readmodel;

import java.util.List;

/**
 * 메뉴 계층의 단일 항목을 표현한다.
 *
 * <p>메뉴는 하나 이상의 Capability(Feature + Action)를 참조할 수 있으며,
 * 사용자가 해당 Capability 중 하나라도 보유하면 메뉴가 표시된다.</p>
 */
public record MenuItem(
        String code,
        String name,
        String featureCode,
        String actionCode,
        String path,
        String icon,
        Integer sortOrder,
        String parentCode,
        String description,
        List<MenuCapabilityRef> requiredCapabilities
) {
    /**
     * 하위 호환성을 위한 간단한 생성자.
     */
    public MenuItem(String code, String name, String featureCode, String actionCode, String path) {
        this(code, name, featureCode, actionCode, path, null, null, null, null, List.of());
    }

    /**
     * 메뉴에 필요한 Capability 참조.
     */
    public record MenuCapabilityRef(String feature, String action) {}
}
