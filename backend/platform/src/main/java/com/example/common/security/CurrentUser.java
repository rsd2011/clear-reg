package com.example.common.security;

import java.util.Map;
import java.util.Optional;

import com.example.common.masking.MaskRuleDefinition;

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
                          Map<String, MaskRuleDefinition> maskRules) {

    public CurrentUser {
        maskRules = maskRules == null ? Map.of() : Map.copyOf(maskRules);
    }

    public Optional<MaskRuleDefinition> maskRule(String tag) {
        return Optional.ofNullable(maskRules.get(tag));
    }
}
