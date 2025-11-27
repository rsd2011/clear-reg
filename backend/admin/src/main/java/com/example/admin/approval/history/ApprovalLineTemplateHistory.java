package com.example.admin.approval.history;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 승인선 템플릿 변경 이력 엔티티.
 * 템플릿의 생성, 수정, 삭제, 복사, 복원 이력을 JSON 스냅샷으로 저장합니다.
 */
@Entity
@Table(name = "approval_line_template_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalLineTemplateHistory extends PrimaryKeyEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private HistoryAction action;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_by_name", length = 100)
    private String changedByName;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(name = "previous_snapshot", columnDefinition = "jsonb")
    private String previousSnapshot;

    @Column(name = "current_snapshot", columnDefinition = "jsonb")
    private String currentSnapshot;

    /** 복사 시 원본 템플릿 ID (COPY 액션에서만 사용) */
    @Column(name = "source_template_id")
    private UUID sourceTemplateId;

    private ApprovalLineTemplateHistory(UUID templateId,
                                        HistoryAction action,
                                        String changedBy,
                                        String changedByName,
                                        OffsetDateTime changedAt,
                                        String previousSnapshot,
                                        String currentSnapshot,
                                        UUID sourceTemplateId) {
        this.templateId = templateId;
        this.action = action;
        this.changedBy = changedBy;
        this.changedByName = changedByName;
        this.changedAt = changedAt;
        this.previousSnapshot = previousSnapshot;
        this.currentSnapshot = currentSnapshot;
        this.sourceTemplateId = sourceTemplateId;
    }

    /**
     * CREATE 이력 생성.
     */
    public static ApprovalLineTemplateHistory createHistory(UUID templateId,
                                                            String changedBy,
                                                            String changedByName,
                                                            OffsetDateTime changedAt,
                                                            String currentSnapshot) {
        return new ApprovalLineTemplateHistory(
                templateId, HistoryAction.CREATE, changedBy, changedByName,
                changedAt, null, currentSnapshot, null);
    }

    /**
     * UPDATE 이력 생성.
     */
    public static ApprovalLineTemplateHistory updateHistory(UUID templateId,
                                                            String changedBy,
                                                            String changedByName,
                                                            OffsetDateTime changedAt,
                                                            String previousSnapshot,
                                                            String currentSnapshot) {
        return new ApprovalLineTemplateHistory(
                templateId, HistoryAction.UPDATE, changedBy, changedByName,
                changedAt, previousSnapshot, currentSnapshot, null);
    }

    /**
     * DELETE (비활성화) 이력 생성.
     */
    public static ApprovalLineTemplateHistory deleteHistory(UUID templateId,
                                                            String changedBy,
                                                            String changedByName,
                                                            OffsetDateTime changedAt,
                                                            String previousSnapshot,
                                                            String currentSnapshot) {
        return new ApprovalLineTemplateHistory(
                templateId, HistoryAction.DELETE, changedBy, changedByName,
                changedAt, previousSnapshot, currentSnapshot, null);
    }

    /**
     * COPY 이력 생성.
     */
    public static ApprovalLineTemplateHistory copyHistory(UUID templateId,
                                                          String changedBy,
                                                          String changedByName,
                                                          OffsetDateTime changedAt,
                                                          String currentSnapshot,
                                                          UUID sourceTemplateId) {
        return new ApprovalLineTemplateHistory(
                templateId, HistoryAction.COPY, changedBy, changedByName,
                changedAt, null, currentSnapshot, sourceTemplateId);
    }

    /**
     * RESTORE (활성화) 이력 생성.
     */
    public static ApprovalLineTemplateHistory restoreHistory(UUID templateId,
                                                             String changedBy,
                                                             String changedByName,
                                                             OffsetDateTime changedAt,
                                                             String previousSnapshot,
                                                             String currentSnapshot) {
        return new ApprovalLineTemplateHistory(
                templateId, HistoryAction.RESTORE, changedBy, changedByName,
                changedAt, previousSnapshot, currentSnapshot, null);
    }
}
