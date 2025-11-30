package com.example.admin.maskingpolicy.dto;

import java.time.Instant;
import java.util.Set;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 마스킹 정책 생성/수정 요청 DTO.
 */
public record MaskingPolicyRootRequest(
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

        Set<String> dataKinds,

        Boolean maskingEnabled,

        Boolean auditEnabled,

        Integer priority,

        boolean active,

        Instant effectiveFrom,

        Instant effectiveTo
) {
    public MaskingPolicyRootRequest {
        if (maskingEnabled == null) {
            maskingEnabled = true;
        }
        if (auditEnabled == null) {
            auditEnabled = false;
        }
        if (priority == null) {
            priority = 100;
        }
    }
}
