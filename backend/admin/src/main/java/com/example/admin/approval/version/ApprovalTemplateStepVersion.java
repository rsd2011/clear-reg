package com.example.admin.approval.version;

import java.util.UUID;

import com.example.admin.approval.ApprovalGroup;
import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 승인선 템플릿 단계 버전 엔티티.
 * <p>
 * 각 템플릿 버전에 속한 승인 단계를 저장합니다.
 * ApprovalGroup의 코드와 이름을 비정규화하여 시점 조회 시 JOIN을 최소화합니다.
 * </p>
 */
@Entity
@Table(name = "approval_template_step_versions", indexes = {
        @Index(name = "idx_atsv_version_order", columnList = "template_version_id, step_order", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplateStepVersion extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private ApprovalLineTemplateVersion templateVersion;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_group_id", nullable = false)
    private ApprovalGroup approvalGroup;

    /** 비정규화: 시점 조회 시 JOIN 최소화 */
    @Column(name = "approval_group_code", nullable = false, length = 64)
    private String approvalGroupCode;

    /** 비정규화: 시점 조회 시 JOIN 최소화 */
    @Column(name = "approval_group_name", nullable = false, length = 255)
    private String approvalGroupName;

    private ApprovalTemplateStepVersion(ApprovalLineTemplateVersion templateVersion,
                                        int stepOrder,
                                        ApprovalGroup approvalGroup,
                                        String approvalGroupCode,
                                        String approvalGroupName) {
        this.templateVersion = templateVersion;
        this.stepOrder = stepOrder;
        this.approvalGroup = approvalGroup;
        this.approvalGroupCode = approvalGroupCode;
        this.approvalGroupName = approvalGroupName;
    }

    /**
     * Step 버전 생성.
     *
     * @param templateVersion 소속 템플릿 버전
     * @param stepOrder       단계 순서
     * @param approvalGroup   승인 그룹
     * @return 새 Step 버전
     */
    public static ApprovalTemplateStepVersion create(ApprovalLineTemplateVersion templateVersion,
                                                     int stepOrder,
                                                     ApprovalGroup approvalGroup) {
        return new ApprovalTemplateStepVersion(
                templateVersion,
                stepOrder,
                approvalGroup,
                approvalGroup.getGroupCode(),
                approvalGroup.getName());
    }

    /**
     * 기존 Step에서 복사하여 새 버전에 추가.
     *
     * @param templateVersion 새 템플릿 버전
     * @param sourceStep      원본 Step 버전
     * @return 복사된 Step 버전
     */
    public static ApprovalTemplateStepVersion copyFrom(ApprovalLineTemplateVersion templateVersion,
                                                       ApprovalTemplateStepVersion sourceStep) {
        return new ApprovalTemplateStepVersion(
                templateVersion,
                sourceStep.getStepOrder(),
                sourceStep.getApprovalGroup(),
                sourceStep.getApprovalGroupCode(),
                sourceStep.getApprovalGroupName());
    }

    /**
     * 승인 그룹 ID 반환.
     */
    public UUID getApprovalGroupId() {
        return approvalGroup.getId();
    }
}
