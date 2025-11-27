package com.example.draft.domain;

import java.time.OffsetDateTime;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.draft.domain.exception.DraftWorkflowException;
import com.example.admin.approval.ApprovalTemplateStep;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "draft_approval_steps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftApprovalStep extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id", nullable = false)
    private Draft draft;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "approval_group_code", nullable = false, length = 64)
    private String approvalGroupCode;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private DraftApprovalState state = DraftApprovalState.WAITING;

    @Column(name = "acted_by", length = 100)
    private String actedBy;

    @Column(name = "acted_at")
    private OffsetDateTime actedAt;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "delegated_to", length = 100)
    private String delegatedTo;

    @Column(name = "delegated_at")
    private OffsetDateTime delegatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    private DraftApprovalStep(int stepOrder, String approvalGroupCode, String description) {
        this.stepOrder = stepOrder;
        this.approvalGroupCode = approvalGroupCode;
        this.description = description;
    }

    public static DraftApprovalStep fromTemplate(ApprovalTemplateStep templateStep) {
        return new DraftApprovalStep(templateStep.getStepOrder(),
                templateStep.getApprovalGroupCode(),
                templateStep.getDescription());
    }

    void attachTo(Draft draft) {
        this.draft = draft;
    }

    void start(OffsetDateTime now) {
        if (this.state != DraftApprovalState.WAITING) {
            return;
        }
        this.state = DraftApprovalState.IN_PROGRESS;
        this.actedAt = now;
    }

    public void approve(String actor, String comment, OffsetDateTime now) {
        ensureInProgress();
        this.state = DraftApprovalState.APPROVED;
        this.actedBy = actor;
        this.actedAt = now;
        this.comment = comment;
    }

    public void reject(String actor, String comment, OffsetDateTime now) {
        ensureInProgress();
        this.state = DraftApprovalState.REJECTED;
        this.actedBy = actor;
        this.actedAt = now;
        this.comment = comment;
    }

    public void defer(String actor, String comment, OffsetDateTime now) {
        ensureInProgress();
        this.state = DraftApprovalState.DEFERRED;
        this.actedBy = actor;
        this.actedAt = now;
        this.comment = comment;
    }

    public void completeDeferred(String actor, String comment, OffsetDateTime now) {
        if (this.state != DraftApprovalState.DEFERRED) {
            throw new DraftWorkflowException("후결재 대상이 아닙니다.");
        }
        this.state = DraftApprovalState.APPROVED;
        this.actedBy = actor;
        this.actedAt = now;
        this.comment = comment;
    }

    public void skip(String reason, OffsetDateTime now) {
        if (this.state.isCompleted()) {
            return;
        }
        this.state = DraftApprovalState.SKIPPED;
        this.comment = reason;
        this.actedAt = now;
    }

    public void delegateTo(String delegatedTo, String delegateComment, OffsetDateTime now) {
        if (this.state.isCompleted()) {
            throw new DraftWorkflowException("이미 완료된 결재 단계는 위임할 수 없습니다.");
        }
        this.delegatedTo = delegatedTo;
        this.delegatedAt = now;
        this.comment = delegateComment;
    }

    public void reset() {
        this.state = DraftApprovalState.WAITING;
        this.actedBy = null;
        this.actedAt = null;
        this.comment = null;
        this.delegatedTo = null;
        this.delegatedAt = null;
    }

    private void ensureInProgress() {
        if (this.state != DraftApprovalState.IN_PROGRESS) {
            throw new DraftWorkflowException("현재 결재 단계는 처리할 수 없습니다.");
        }
    }
}
