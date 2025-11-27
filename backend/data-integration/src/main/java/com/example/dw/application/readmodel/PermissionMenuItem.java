package com.example.dw.application.readmodel;

import java.util.List;

/**
 * 사용자별 메뉴 항목. 권한 기반 필터링이 적용된 메뉴 정보를 담는다.
 */
public record PermissionMenuItem(
        String code,
        String name,
        String featureCode,
        String actionCode,
        String path,
        String icon,
        Integer sortOrder,
        String parentCode,
        String description,
        List<MenuItem.MenuCapabilityRef> grantedCapabilities
) {
    /**
     * 하위 호환성을 위한 간단한 생성자.
     */
    public PermissionMenuItem(String code, String name, String featureCode, String actionCode,
                               String path) {
        this(code, name, featureCode, actionCode, path, null, null, null, null, List.of());
    }
}
