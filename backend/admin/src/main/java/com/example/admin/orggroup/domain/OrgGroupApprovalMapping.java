package com.example.admin.orggroup.domain;

import java.time.OffsetDateTime;

import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.orggroup.WorkType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 조직그룹별 승인선 템플릿 매핑.
 *
 * <p>조직그룹과 업무유형 조합에 따라 적용할 승인선 템플릿을 지정한다.</p>
 * <p>workType이 null인 경우 해당 조직그룹의 기본 템플릿으로 사용된다.</p>
 */
@Entity
@Table(
        name = "org_group_approval_mapping",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_org_group_work_type",
                columnNames = {"org_group_id", "work_type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrgGroupApprovalMapping extends PrimaryKeyEntity {

    /**
     * 조직그룹 (FK 참조).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_group_id", nullable = false)
    private OrgGroup orgGroup;

    /**
     * 업무유형.
     * <p>null인 경우 해당 조직그룹의 기본(default) 템플릿을 의미한다.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", length = 50)
    private WorkType workType;

    /**
     * 적용할 승인선 템플릿.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_template_root_id", nullable = false)
    private ApprovalTemplateRoot approvalTemplateRoot;

    /**
     * 적용할 기안 양식 템플릿 (선택).
     * <p>기안 생성 시 사용할 양식을 정의한다.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_form_template_root_id")
    private DraftFormTemplateRoot draftFormTemplateRoot;

    /**
     * 생성 일시.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * 수정 일시.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 생성자.
     *
     * @param orgGroup              조직그룹 (필수)
     * @param workType              업무유형 (null 허용 - 기본 템플릿)
     * @param approvalTemplateRoot  승인선 템플릿 (필수)
     * @param draftFormTemplateRoot 기안 양식 템플릿 (선택)
     * @param now                   생성 시점
     */
    private OrgGroupApprovalMapping(
            OrgGroup orgGroup,
            WorkType workType,
            ApprovalTemplateRoot approvalTemplateRoot,
            DraftFormTemplateRoot draftFormTemplateRoot,
            OffsetDateTime now) {
        this.orgGroup = orgGroup;
        this.workType = workType;
        this.approvalTemplateRoot = approvalTemplateRoot;
        this.draftFormTemplateRoot = draftFormTemplateRoot;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 매핑을 생성한다.
     *
     * @param orgGroup              조직그룹
     * @param workType              업무유형 (null 허용)
     * @param approvalTemplateRoot  승인선 템플릿
     * @param draftFormTemplateRoot 기안 양식 템플릿 (선택)
     * @param now                   생성 시점
     * @return 새로운 매핑 인스턴스
     */
    public static OrgGroupApprovalMapping create(
            OrgGroup orgGroup,
            WorkType workType,
            ApprovalTemplateRoot approvalTemplateRoot,
            DraftFormTemplateRoot draftFormTemplateRoot,
            OffsetDateTime now) {
        if (orgGroup == null) {
            throw new IllegalArgumentException("조직그룹은 필수입니다.");
        }
        if (approvalTemplateRoot == null) {
            throw new IllegalArgumentException("승인선 템플릿은 필수입니다.");
        }
        return new OrgGroupApprovalMapping(orgGroup, workType, approvalTemplateRoot, draftFormTemplateRoot, now);
    }

    /**
     * 매핑을 생성한다 (기안 양식 템플릿 없이).
     *
     * @param orgGroup             조직그룹
     * @param workType             업무유형 (null 허용)
     * @param approvalTemplateRoot 승인선 템플릿
     * @param now                  생성 시점
     * @return 새로운 매핑 인스턴스
     */
    public static OrgGroupApprovalMapping create(
            OrgGroup orgGroup,
            WorkType workType,
            ApprovalTemplateRoot approvalTemplateRoot,
            OffsetDateTime now) {
        return create(orgGroup, workType, approvalTemplateRoot, null, now);
    }

    /**
     * 기본 템플릿 매핑을 생성한다.
     *
     * @param orgGroup             조직그룹
     * @param approvalTemplateRoot 기본 승인선 템플릿
     * @param now                  생성 시점
     * @return 새로운 기본 매핑 인스턴스
     */
    public static OrgGroupApprovalMapping createDefault(
            OrgGroup orgGroup,
            ApprovalTemplateRoot approvalTemplateRoot,
            OffsetDateTime now) {
        return create(orgGroup, null, approvalTemplateRoot, null, now);
    }

    /**
     * 템플릿을 변경한다.
     *
     * @param newTemplate 새 템플릿
     * @param now         변경 시점
     */
    public void changeTemplate(ApprovalTemplateRoot newTemplate, OffsetDateTime now) {
        if (newTemplate == null) {
            throw new IllegalArgumentException("승인선 템플릿은 필수입니다.");
        }
        this.approvalTemplateRoot = newTemplate;
        this.updatedAt = now;
    }

    /**
     * 기안 양식 템플릿을 변경한다.
     *
     * @param newFormTemplateRoot 새 기안 양식 템플릿 (null 허용)
     * @param now                 변경 시점
     */
    public void changeDraftFormTemplateRoot(DraftFormTemplateRoot newFormTemplateRoot, OffsetDateTime now) {
        this.draftFormTemplateRoot = newFormTemplateRoot;
        this.updatedAt = now;
    }

    /**
     * 기안 양식 템플릿이 설정되어 있는지 확인한다.
     *
     * @return 템플릿이 설정되어 있으면 true
     */
    public boolean hasDraftFormTemplateRoot() {
        return this.draftFormTemplateRoot != null;
    }

    /**
     * 기본 템플릿 매핑인지 확인한다.
     *
     * @return workType이 null이면 true
     */
    public boolean isDefault() {
        return this.workType == null;
    }
}
