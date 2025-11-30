package com.example.admin.orggroup.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.admin.orggroup.domain.OrgGroupApprovalMapping;
import com.example.common.orggroup.WorkType;

/**
 * 조직그룹 승인선 매핑 응답 DTO.
 *
 * @param id                     매핑 ID
 * @param orgGroupId             조직그룹 ID
 * @param orgGroupCode           조직그룹 코드
 * @param orgGroupName           조직그룹 이름
 * @param workType               업무유형 (null이면 기본 템플릿)
 * @param workTypeDisplayName    업무유형 표시명
 * @param approvalTemplateRootId 승인선 템플릿 루트 ID
 * @param templateCode           템플릿 코드
 * @param templateName           템플릿 이름
 * @param isDefault              기본 템플릿 여부
 * @param createdAt              생성 일시
 * @param updatedAt              수정 일시
 */
public record OrgGroupApprovalMappingResponse(
        UUID id,
        UUID orgGroupId,
        String orgGroupCode,
        String orgGroupName,
        WorkType workType,
        String workTypeDisplayName,
        UUID approvalTemplateRootId,
        String templateCode,
        String templateName,
        boolean isDefault,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 매핑 엔티티를 응답 DTO로 변환한다.
     */
    public static OrgGroupApprovalMappingResponse from(OrgGroupApprovalMapping mapping) {
        var orgGroup = mapping.getOrgGroup();
        var templateRoot = mapping.getApprovalTemplateRoot();
        var workType = mapping.getWorkType();

        return new OrgGroupApprovalMappingResponse(
                mapping.getId(),
                orgGroup.getId(),
                orgGroup.getCode(),
                orgGroup.getName(),
                workType,
                workType != null ? workType.name() : "기본",
                templateRoot.getId(),
                templateRoot.getTemplateCode(),
                templateRoot.getName(),
                mapping.isDefault(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt()
        );
    }
}
