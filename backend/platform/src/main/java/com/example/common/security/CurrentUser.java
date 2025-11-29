package com.example.common.security;

import java.util.List;

/**
 * 인증/인가 컨텍스트를 표현하는 플랫폼 공통 계약.
 * 구체 구현(auth 모듈 등)은 이 타입으로 노출한다.
 */
public record CurrentUser(String username,
                          String organizationCode,
                          String permissionGroupCode,
                          String featureCode,
                          String actionCode,
                          RowScope rowScope,
                          List<String> orgGroupCodes) {

    public CurrentUser {
        orgGroupCodes = orgGroupCodes == null ? List.of() : List.copyOf(orgGroupCodes);
    }

    /**
     * 역호환성을 위한 생성자 - orgGroupCodes 없이 생성.
     */
    public CurrentUser(String username, String organizationCode, String permissionGroupCode,
                       String featureCode, String actionCode, RowScope rowScope) {
        this(username, organizationCode, permissionGroupCode, featureCode, actionCode,
             rowScope, List.of());
    }
}
