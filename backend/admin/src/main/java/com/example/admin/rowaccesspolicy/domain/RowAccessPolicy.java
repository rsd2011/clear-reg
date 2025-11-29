package com.example.admin.rowaccesspolicy.domain;

import java.time.Instant;
import java.time.OffsetDateTime;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.security.RowScope;
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
 * 행 접근 정책 엔티티 (SCD Type 2 버전 관리).
 * <p>
 * 정책의 각 버전을 저장하여 시점 조회와 감사 추적을 지원합니다.
 * 사용자가 조회할 수 있는 데이터의 행 범위를 정의합니다.
 * RowScope에 따라 자신의 데이터만(OWN), 조직 데이터(ORG), 전체 데이터(ALL) 등을 조회할 수 있습니다.
 * </p>
 */
@Entity
@Table(name = "row_access_policies", indexes = {
        @Index(name = "idx_rap_root_version", columnList = "root_id, version"),
        @Index(name = "idx_rap_valid_range", columnList = "root_id, valid_from, valid_to"),
        @Index(name = "idx_rap_feature_action", columnList = "feature_code, action_code"),
        @Index(name = "idx_rap_priority", columnList = "priority")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RowAccessPolicy extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_id", nullable = false)
    private RowAccessPolicyRoot root;

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

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_code", nullable = false, length = 100)
    private FeatureCode featureCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_code", length = 100)
    private ActionCode actionCode;

    @Column(name = "perm_group_code", length = 100)
    private String permGroupCode;

    @Column(name = "org_group_code", length = 100)
    private String orgGroupCode;

    /**
     * 행 접근 범위.
     * OWN: 자신이 생성한 데이터만
     * ORG: 자신의 조직 데이터
     * ALL: 전체 데이터
     * CUSTOM: 커스텀 규칙
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "row_scope", nullable = false, length = 30)
    private RowScope rowScope;

    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

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

    /** 버전 태그 (선택적) */
    @Column(name = "version_tag", length = 100)
    private String versionTag;

    // === 생성자 ===

    private RowAccessPolicy(RowAccessPolicyRoot root,
                            Integer version,
                            String name,
                            String description,
                            FeatureCode featureCode,
                            ActionCode actionCode,
                            String permGroupCode,
                            String orgGroupCode,
                            RowScope rowScope,
                            Integer priority,
                            boolean active,
                            Instant effectiveFrom,
                            Instant effectiveTo,
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
        this.description = description;
        this.featureCode = featureCode;
        this.actionCode = actionCode;
        this.permGroupCode = permGroupCode;
        this.orgGroupCode = orgGroupCode;
        this.rowScope = rowScope;
        this.priority = priority != null ? priority : 100;
        this.active = active;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.status = status;
        this.changeAction = changeAction;
        this.changeReason = changeReason;
        this.changedBy = changedBy;
        this.changedByName = changedByName;
        this.changedAt = now;
        this.rollbackFromVersion = rollbackFromVersion;
    }

    /**
     * 새 버전 생성 (일반적인 생성/수정/삭제 등).
     */
    public static RowAccessPolicy create(RowAccessPolicyRoot root,
                                         Integer version,
                                         String name,
                                         String description,
                                         FeatureCode featureCode,
                                         ActionCode actionCode,
                                         String permGroupCode,
                                         String orgGroupCode,
                                         RowScope rowScope,
                                         Integer priority,
                                         boolean active,
                                         Instant effectiveFrom,
                                         Instant effectiveTo,
                                         ChangeAction changeAction,
                                         String changeReason,
                                         String changedBy,
                                         String changedByName,
                                         OffsetDateTime now) {
        return new RowAccessPolicy(
                root, version, name, description,
                featureCode, actionCode, permGroupCode, orgGroupCode,
                rowScope, priority, active,
                effectiveFrom, effectiveTo,
                VersionStatus.PUBLISHED, changeAction, changeReason,
                changedBy, changedByName, now, null);
    }

    /**
     * 초안(Draft) 버전 생성.
     */
    public static RowAccessPolicy createDraft(RowAccessPolicyRoot root,
                                              Integer version,
                                              String name,
                                              String description,
                                              FeatureCode featureCode,
                                              ActionCode actionCode,
                                              String permGroupCode,
                                              String orgGroupCode,
                                              RowScope rowScope,
                                              Integer priority,
                                              boolean active,
                                              Instant effectiveFrom,
                                              Instant effectiveTo,
                                              String changeReason,
                                              String changedBy,
                                              String changedByName,
                                              OffsetDateTime now) {
        return new RowAccessPolicy(
                root, version, name, description,
                featureCode, actionCode, permGroupCode, orgGroupCode,
                rowScope, priority, active,
                effectiveFrom, effectiveTo,
                VersionStatus.DRAFT, ChangeAction.DRAFT, changeReason,
                changedBy, changedByName, now, null);
    }

    /**
     * 롤백 시 버전 생성.
     */
    public static RowAccessPolicy createFromRollback(RowAccessPolicyRoot root,
                                                     Integer version,
                                                     String name,
                                                     String description,
                                                     FeatureCode featureCode,
                                                     ActionCode actionCode,
                                                     String permGroupCode,
                                                     String orgGroupCode,
                                                     RowScope rowScope,
                                                     Integer priority,
                                                     boolean active,
                                                     Instant effectiveFrom,
                                                     Instant effectiveTo,
                                                     String changeReason,
                                                     String changedBy,
                                                     String changedByName,
                                                     OffsetDateTime now,
                                                     Integer rollbackFromVersion) {
        return new RowAccessPolicy(
                root, version, name, description,
                featureCode, actionCode, permGroupCode, orgGroupCode,
                rowScope, priority, active,
                effectiveFrom, effectiveTo,
                VersionStatus.PUBLISHED, ChangeAction.ROLLBACK, changeReason,
                changedBy, changedByName, now, rollbackFromVersion);
    }

    // === 상태 확인 메서드 ===

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
                            String description,
                            FeatureCode featureCode,
                            ActionCode actionCode,
                            String permGroupCode,
                            String orgGroupCode,
                            RowScope rowScope,
                            Integer priority,
                            boolean active,
                            Instant effectiveFrom,
                            Instant effectiveTo,
                            String changeReason,
                            OffsetDateTime now) {
        if (this.status != VersionStatus.DRAFT) {
            throw new IllegalStateException("초안 상태에서만 수정할 수 있습니다. 현재 상태: " + this.status);
        }
        this.name = name;
        this.description = description;
        this.featureCode = featureCode;
        this.actionCode = actionCode;
        this.permGroupCode = permGroupCode;
        this.orgGroupCode = orgGroupCode;
        this.rowScope = rowScope;
        this.priority = priority != null ? priority : this.priority;
        this.active = active;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.changeReason = changeReason;
        this.changedAt = now;
    }

    /**
     * 버전 태그 설정.
     */
    public void setVersionTag(String tag) {
        this.versionTag = tag;
    }

    // === 정책 매칭 로직 ===

    /**
     * 지정된 시점에 유효한지 확인.
     */
    public boolean isEffectiveAt(Instant ts) {
        if (!active) {
            return false;
        }
        if (effectiveFrom != null && ts.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveTo != null && !ts.isBefore(effectiveTo)) {
            return false;
        }
        return true;
    }

    /**
     * 정책 매칭 확인.
     */
    public boolean matches(FeatureCode feature, ActionCode action, String permGroup, Instant ts) {
        if (!featureCode.equals(feature)) return false;

        if (actionCode != null) {
            if (action == null || !actionCode.equals(action)) return false;
        } else if (action != null) {
            return false;
        }

        if (permGroupCode != null) {
            if (permGroup == null || !permGroupCode.equals(permGroup)) return false;
        } else if (permGroup != null) {
            return false;
        }

        return isEffectiveAt(ts);
    }
}
