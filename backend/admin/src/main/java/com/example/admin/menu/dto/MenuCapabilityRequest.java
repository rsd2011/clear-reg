package com.example.admin.menu.dto;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import jakarta.validation.constraints.NotNull;

/**
 * 메뉴 Capability 요청 DTO.
 *
 * @param feature 기능 코드
 * @param action 액션 코드
 */
public record MenuCapabilityRequest(
        @NotNull(message = "기능 코드는 필수입니다")
        FeatureCode feature,

        @NotNull(message = "액션 코드는 필수입니다")
        ActionCode action
) {
}
