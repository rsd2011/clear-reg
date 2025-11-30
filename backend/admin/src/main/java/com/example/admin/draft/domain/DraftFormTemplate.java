package com.example.admin.draft.domain;

import java.time.OffsetDateTime;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.orggroup.WorkType;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기안 양식 템플릿 엔티티 (SCD Type 2 버전 관리).
 * <p>
 * 템플릿의 각 버전을 저장하여 시점 조회와 감사 추적을 지원합니다.
 * 비즈니스 데이터(name, workType, schemaJson, active)를 포함합니다.
 * </p>
 */
@Entity
@Table(name = "draft_form_templates", indexes = {
        @Index(name = "idx_dft_root_version", columnList = "root_id, version"),
        @Index(name = "idx_dft_valid_range", columnList = "root_id, valid_from, valid_to")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftFormTemplate extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_id", nullable = false)
    private DraftFormTemplateRoot root;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 50)
    private WorkType workType;

    @Column(name = "schema_json", nullable = false, columnDefinition = "text")
    private String schemaJson;

    @Column(name = "active", nullable = false)
    private boolean active = true;

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

    /** ROLLBACK 시 롤백 대상 버전 번호 */
    @Column(name = "rollback_from_version")
    private Integer rollbackFromVersion;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    private DraftFormTemplate(DraftFormTemplateRoot root,
                              Integer version,
                              String name,
                              WorkType workType,
                              String schemaJson,
                              boolean active,
                              VersionStatus status,
                              ChangeAction changeAction,
                              String changeReason,
                              String changedBy,
                              String changedByName,
                              OffsetDateTime now,
                              Integer rollbackFromVersion) {
        this.root = root;
        this.version = version;
        this.validFrom = now;
        this.validTo = null;
        this.name = name;
        this.workType = workType;
        this.schemaJson = schemaJson;
        this.active = active;
        this.status = status;
        this.changeAction = changeAction;
        this.changeReason = changeReason;
        this.changedBy = changedBy;
        this.changedByName = changedByName;
        this.changedAt = now;
        this.rollbackFromVersion = rollbackFromVersion;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 새 버전 생성 (일반적인 생성/수정/삭제 등).
     */
    public static DraftFormTemplate create(DraftFormTemplateRoot root,
                                           Integer version,
                                           String name,
                                           WorkType workType,
                                           String schemaJson,
                                           boolean active,
                                           ChangeAction changeAction,
                                           String changeReason,
                                           String changedBy,
                                           String changedByName,
                                           OffsetDateTime now) {
        return new DraftFormTemplate(
                root, version, name, workType, schemaJson, active,
                VersionStatus.PUBLISHED, changeAction, changeReason,
                changedBy, changedByName, now, null);
    }

    /**
     * 초안(Draft) 버전 생성.
     */
    public static DraftFormTemplate createDraft(DraftFormTemplateRoot root,
                                                Integer version,
                                                String name,
                                                WorkType workType,
                                                String schemaJson,
                                                boolean active,
                                                String changeReason,
                                                String changedBy,
                                                String changedByName,
                                                OffsetDateTime now) {
        return new DraftFormTemplate(
                root, version, name, workType, schemaJson, active,
                VersionStatus.DRAFT, ChangeAction.DRAFT, changeReason,
                changedBy, changedByName, now, null);
    }

    /**
     * 롤백 시 버전 생성.
     */
    public static DraftFormTemplate createFromRollback(DraftFormTemplateRoot root,
                                                       Integer version,
                                                       String name,
                                                       WorkType workType,
                                                       String schemaJson,
                                                       boolean active,
                                                       String changeReason,
                                                       String changedBy,
                                                       String changedByName,
                                                       OffsetDateTime now,
                                                       Integer rollbackFromVersion) {
        return new DraftFormTemplate(
                root, version, name, workType, schemaJson, active,
                VersionStatus.PUBLISHED, ChangeAction.ROLLBACK, changeReason,
                changedBy, changedByName, now, rollbackFromVersion);
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
        this.updatedAt = closedAt;
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
        this.updatedAt = publishedAt;
    }

    /**
     * 초안 내용 업데이트.
     */
    public void updateDraft(String name,
                            WorkType workType,
                            String schemaJson,
                            boolean active,
                            String changeReason,
                            OffsetDateTime now) {
        if (this.status != VersionStatus.DRAFT) {
            throw new IllegalStateException("초안 상태에서만 수정할 수 있습니다. 현재 상태: " + this.status);
        }
        this.name = name;
        this.workType = workType;
        this.schemaJson = schemaJson;
        this.active = active;
        this.changeReason = changeReason;
        this.changedAt = now;
        this.updatedAt = now;
    }

    /**
     * 템플릿 코드를 반환합니다. (Root의 templateCode)
     */
    public String getTemplateCode() {
        return root != null ? root.getTemplateCode() : null;
    }
}
