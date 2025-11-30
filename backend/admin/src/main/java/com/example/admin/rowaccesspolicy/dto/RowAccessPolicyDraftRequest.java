package com.example.admin.rowaccesspolicy.dto;

import java.time.Instant;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.security.RowScope;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 행 접근 정책 초안 저장 요청 DTO.
 */
public record RowAccessPolicyDraftRequest(
        @NotBlank(message = "정책 이름은 필수입니다.")
        @Size(max = 255)
        String name,

        @Size(max = 500)
        String description,

        @NotNull(message = "기능 코드는 필수입니다.")
        FeatureCode featureCode,

        ActionCode actionCode,

        @Size(max = 100)
        String permGroupCode,

        @Size(max = 100)
        String orgGroupCode,

        @NotNull(message = "행 접근 범위는 필수입니다.")
        RowScope rowScope,

        Integer priority,

        boolean active,

        Instant effectiveFrom,

        Instant effectiveTo,

        @Size(max = 500)
        String changeReason
) {
    public RowAccessPolicyDraftRequest {
        if (priority == null) {
            priority = 100;
        }
    }
}
