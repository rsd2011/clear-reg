package com.example.admin.orggroup.dto;

import java.util.UUID;

import com.example.common.orggroup.WorkType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 조직그룹 승인선 매핑 생성/수정 요청 DTO.
 *
 * @param orgGroupCode           조직그룹 코드 (필수)
 * @param workType               업무유형 (null이면 기본 템플릿)
 * @param approvalTemplateRootId 승인선 템플릿 루트 ID (필수)
 */
public record OrgGroupApprovalMappingRequest(
        @NotBlank(message = "조직그룹 코드는 필수입니다")
        String orgGroupCode,

        WorkType workType,

        @NotNull(message = "승인선 템플릿 ID는 필수입니다")
        UUID approvalTemplateRootId
) {
}
