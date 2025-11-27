package com.example.common.security;

import java.util.List;
import java.util.Optional;

/**
 * 인증/인가 컨텍스트를 표현하는 플랫폼 공통 계약.
 * 구체 구현(auth 모듈 등)은 이 타입으로 노출한다.
 *
 * <p>DataPolicy 기반 마스킹을 위해 orgPolicyId, orgGroupCodes, businessType 필드가 추가됨.
 */
public record CurrentUser(String username,
                          String organizationCode,
                          String permissionGroupCode,
                          String featureCode,
                          String actionCode,
                          RowScope rowScope,
                          Long orgPolicyId,
                          List<String> orgGroupCodes,
                          String businessType) {

    public CurrentUser {
        orgGroupCodes = orgGroupCodes == null ? List.of() : List.copyOf(orgGroupCodes);
    }

    /**
     * 역호환성을 위한 생성자 - 기존 코드에서 maskRules를 사용하던 경우 대응.
     * @deprecated DataPolicy 기반 마스킹으로 마이그레이션 후 제거 예정
     */
    @Deprecated(forRemoval = true)
    public CurrentUser(String username, String organizationCode, String permissionGroupCode,
                       String featureCode, String actionCode, RowScope rowScope) {
        this(username, organizationCode, permissionGroupCode, featureCode, actionCode,
             rowScope, null, List.of(), null);
    }
}
