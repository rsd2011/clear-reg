package com.example.admin.menu.dto;

import com.example.admin.menu.domain.MenuCapability;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;

/**
 * 메뉴 Capability 응답 DTO.
 *
 * @param feature 기능 코드
 * @param action 액션 코드
 */
public record MenuCapabilityResponse(
        FeatureCode feature,
        ActionCode action
) {
    /**
     * MenuCapability 엔티티를 응답 DTO로 변환한다.
     */
    public static MenuCapabilityResponse from(MenuCapability capability) {
        return new MenuCapabilityResponse(
                capability.getFeature(),
                capability.getAction()
        );
    }
}
