package com.example.admin.approval.domain;

import java.util.UUID;

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
 * 승인선 템플릿 단계 엔티티.
 * <p>
 * 각 템플릿에 속한 승인 단계를 저장합니다.
 * ApprovalGroup의 코드와 이름을 비정규화하여 시점 조회 시 JOIN을 최소화합니다.
 * </p>
 */
@Entity
@Table(name = "approval_template_steps", indexes = {
        @Index(name = "idx_ats_template_order", columnList = "template_id, step_order", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplateStep extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ApprovalTemplate template;

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

    /** 스킵 가능 여부: true면 결재자를 지정하지 않아도 승인 프로세스 통과 */
    @Column(name = "skippable", nullable = false)
    private boolean skippable = false;

    private ApprovalTemplateStep(ApprovalTemplate template,
                                 int stepOrder,
                                 ApprovalGroup approvalGroup,
                                 String approvalGroupCode,
                                 String approvalGroupName,
                                 boolean skippable) {
        this.template = template;
        this.stepOrder = stepOrder;
        this.approvalGroup = approvalGroup;
        this.approvalGroupCode = approvalGroupCode;
        this.approvalGroupName = approvalGroupName;
        this.skippable = skippable;
    }

    /**
     * Step 생성.
     *
     * @param template      소속 템플릿
     * @param stepOrder     단계 순서
     * @param approvalGroup 승인 그룹
     * @param skippable     스킵 가능 여부
     * @return 새 Step
     */
    public static ApprovalTemplateStep create(ApprovalTemplate template,
                                              int stepOrder,
                                              ApprovalGroup approvalGroup,
                                              boolean skippable) {
        return new ApprovalTemplateStep(
                template,
                stepOrder,
                approvalGroup,
                approvalGroup.getGroupCode(),
                approvalGroup.getName(),
                skippable);
    }

    /**
     * 기존 Step에서 복사하여 새 템플릿에 추가.
     *
     * @param template   새 템플릿
     * @param sourceStep 원본 Step
     * @return 복사된 Step
     */
    public static ApprovalTemplateStep copyFrom(ApprovalTemplate template,
                                                ApprovalTemplateStep sourceStep) {
        return new ApprovalTemplateStep(
                template,
                sourceStep.getStepOrder(),
                sourceStep.getApprovalGroup(),
                sourceStep.getApprovalGroupCode(),
                sourceStep.getApprovalGroupName(),
                sourceStep.isSkippable());
    }

    /**
     * 승인 그룹 ID 반환.
     */
    public UUID getApprovalGroupId() {
        return approvalGroup.getId();
    }
}
