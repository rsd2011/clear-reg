package com.example.admin.approval.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 승인선 템플릿 버전 엔티티 (SCD Type 2).
 * <p>
 * 템플릿의 각 버전을 저장하여 시점 조회와 감사 추적을 지원합니다.
 * </p>
 */
@Entity
@Table(name = "approval_line_template_versions", indexes = {
        @Index(name = "idx_altv_template_version", columnList = "template_id, version"),
        @Index(name = "idx_altv_valid_range", columnList = "template_id, valid_from, valid_to")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalLineTemplateVersion extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ApprovalLineTemplate template;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    /** NULL이면 현재 유효한 버전 */
    @Column(name = "valid_to")
    private OffsetDateTime validTo;

    // === 비즈니스 필드 (스냅샷) ===

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    // === 버전 상태 (Draft/Published) ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VersionStatus status = VersionStatus.PUBLISHED;

    // === 감사 필드 ===

    @Enumerated(EnumType.STRING)
    @Column(name = "change_action", nullable = false, length = 20)
    private ChangeAction changeAction;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_by_name", length = 100)
    private String changedByName;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    /** COPY 시 원본 템플릿 ID */
    @Column(name = "source_template_id")
    private UUID sourceTemplateId;

    /** ROLLBACK 시 롤백 대상 버전 번호 */
    @Column(name = "rollback_from_version")
    private Integer rollbackFromVersion;

    /** 버전 태그 (선택적) */
    @Column(name = "version_tag", length = 100)
    private String versionTag;

    // === Step 버전들 ===

    @OneToMany(mappedBy = "templateVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private final List<ApprovalTemplateStepVersion> steps = new ArrayList<>();

    private ApprovalLineTemplateVersion(ApprovalLineTemplate template,
                                        Integer version,
                                        String name,
                                        Integer displayOrder,
                                        String description,
                                        boolean active,
                                        VersionStatus status,
                                        ChangeAction changeAction,
                                        String changeReason,
                                        String changedBy,
                                        String changedByName,
                                        OffsetDateTime now,
                                        UUID sourceTemplateId,
                                        Integer rollbackFromVersion) {
        this.template = template;
        this.version = version;
        this.validFrom = now;
        this.validTo = null;
        this.name = name;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.description = description;
        this.active = active;
        this.status = status;
        this.changeAction = changeAction;
        this.changeReason = changeReason;
        this.changedBy = changedBy;
        this.changedByName = changedByName;
        this.changedAt = now;
        this.sourceTemplateId = sourceTemplateId;
        this.rollbackFromVersion = rollbackFromVersion;
    }

    /**
     * 새 버전 생성 (일반적인 생성/수정/삭제 등).
     */
    public static ApprovalLineTemplateVersion create(ApprovalLineTemplate template,
                                                     Integer version,
                                                     String name,
                                                     Integer displayOrder,
                                                     String description,
                                                     boolean active,
                                                     ChangeAction changeAction,
                                                     String changeReason,
                                                     String changedBy,
                                                     String changedByName,
                                                     OffsetDateTime now) {
        return new ApprovalLineTemplateVersion(
                template, version, name, displayOrder, description, active,
                VersionStatus.PUBLISHED, changeAction, changeReason,
                changedBy, changedByName, now, null, null);
    }

    /**
     * 초안(Draft) 버전 생성.
     */
    public static ApprovalLineTemplateVersion createDraft(ApprovalLineTemplate template,
                                                          Integer version,
                                                          String name,
                                                          Integer displayOrder,
                                                          String description,
                                                          boolean active,
                                                          String changeReason,
                                                          String changedBy,
                                                          String changedByName,
                                                          OffsetDateTime now) {
        return new ApprovalLineTemplateVersion(
                template, version, name, displayOrder, description, active,
                VersionStatus.DRAFT, ChangeAction.DRAFT, changeReason,
                changedBy, changedByName, now, null, null);
    }

    /**
     * 복사 시 버전 생성.
     */
    public static ApprovalLineTemplateVersion createFromCopy(ApprovalLineTemplate template,
                                                             Integer version,
                                                             String name,
                                                             Integer displayOrder,
                                                             String description,
                                                             boolean active,
                                                             String changedBy,
                                                             String changedByName,
                                                             OffsetDateTime now,
                                                             UUID sourceTemplateId) {
        return new ApprovalLineTemplateVersion(
                template, version, name, displayOrder, description, active,
                VersionStatus.PUBLISHED, ChangeAction.COPY, null,
                changedBy, changedByName, now, sourceTemplateId, null);
    }

    /**
     * 롤백 시 버전 생성.
     */
    public static ApprovalLineTemplateVersion createFromRollback(ApprovalLineTemplate template,
                                                                 Integer version,
                                                                 String name,
                                                                 Integer displayOrder,
                                                                 String description,
                                                                 boolean active,
                                                                 String changeReason,
                                                                 String changedBy,
                                                                 String changedByName,
                                                                 OffsetDateTime now,
                                                                 Integer rollbackFromVersion) {
        return new ApprovalLineTemplateVersion(
                template, version, name, displayOrder, description, active,
                VersionStatus.PUBLISHED, ChangeAction.ROLLBACK, changeReason,
                changedBy, changedByName, now, null, rollbackFromVersion);
    }

    /**
     * 현재 유효한 버전인지 확인.
     */
    public boolean isCurrent() {
        return this.validTo == null && this.status == VersionStatus.PUBLISHED;
    }

    /**
     * 초안 상태인지 확인.
     */
    public boolean isDraft() {
        return this.status == VersionStatus.DRAFT;
    }

    /**
     * 버전 종료 (새 버전이 생기면 호출).
     */
    public void close(OffsetDateTime closedAt) {
        this.validTo = closedAt;
        if (this.status == VersionStatus.PUBLISHED) {
            this.status = VersionStatus.HISTORICAL;
        }
    }

    /**
     * 초안을 게시 상태로 전환.
     */
    public void publish(OffsetDateTime publishedAt) {
        if (this.status != VersionStatus.DRAFT) {
            throw new IllegalStateException("초안 상태에서만 게시할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = VersionStatus.PUBLISHED;
        this.changeAction = ChangeAction.PUBLISH;
        this.validFrom = publishedAt;
        this.changedAt = publishedAt;
    }

    /**
     * 초안 내용 업데이트.
     */
    public void updateDraft(String name,
                            Integer displayOrder,
                            String description,
                            boolean active,
                            String changeReason,
                            OffsetDateTime now) {
        if (this.status != VersionStatus.DRAFT) {
            throw new IllegalStateException("초안 상태에서만 수정할 수 있습니다. 현재 상태: " + this.status);
        }
        this.name = name;
        this.displayOrder = displayOrder != null ? displayOrder : this.displayOrder;
        this.description = description;
        this.active = active;
        this.changeReason = changeReason;
        this.changedAt = now;
    }

    /**
     * Step 추가.
     */
    public void addStep(ApprovalTemplateStepVersion step) {
        this.steps.add(step);
    }

    /**
     * Steps 교체.
     */
    public void replaceSteps(List<ApprovalTemplateStepVersion> newSteps) {
        this.steps.clear();
        this.steps.addAll(newSteps);
    }

    /**
     * 버전 태그 설정.
     */
    public void setVersionTag(String tag) {
        this.versionTag = tag;
    }
}
