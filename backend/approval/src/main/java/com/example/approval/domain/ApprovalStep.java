package com.example.approval.domain;

import com.example.approval.api.ApprovalStatus;

import java.time.OffsetDateTime;

public class ApprovalStep {
    private final int stepOrder;
    private final String approvalGroupCode;
    private ApprovalStatus status;
    private String actedBy;
    private OffsetDateTime actedAt;
    private String delegatedTo;
    private OffsetDateTime delegatedAt;
    private String delegateComment;

    public ApprovalStep(int stepOrder, String approvalGroupCode) {
        this.stepOrder = stepOrder;
        this.approvalGroupCode = approvalGroupCode;
        this.status = ApprovalStatus.REQUESTED;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public String getApprovalGroupCode() {
        return approvalGroupCode;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public String getActedBy() {
        return actedBy;
    }

    public OffsetDateTime getActedAt() {
        return actedAt;
    }

    public String getDelegatedTo() {
        return delegatedTo;
    }

    public OffsetDateTime getDelegatedAt() {
        return delegatedAt;
    }

    public String getDelegateComment() {
        return delegateComment;
    }

    public void approve(String actor, OffsetDateTime now) {
        transitionTo(ApprovalStatus.APPROVED, actor, now);
    }

    public void reject(String actor, OffsetDateTime now) {
        transitionTo(ApprovalStatus.REJECTED, actor, now);
    }

    public void delegateTo(String delegatedTo, String actor, String comment, OffsetDateTime now) {
        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED) {
            throw new IllegalStateException("Step already completed");
        }
        this.delegatedTo = delegatedTo;
        this.delegatedAt = now;
        this.delegateComment = comment;
        // keep status as is (requested/in-progress)
        this.actedBy = actor;
        this.actedAt = now;
    }

    public void defer(String actor, OffsetDateTime now) {
        transitionTo(ApprovalStatus.DEFERRED, actor, now);
    }

    public void completeDeferred(String actor, OffsetDateTime now) {
        if (status != ApprovalStatus.DEFERRED) {
            throw new IllegalStateException("Step is not deferred");
        }
        transitionTo(ApprovalStatus.APPROVED, actor, now);
    }

    private void transitionTo(ApprovalStatus target, String actor, OffsetDateTime now) {
        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED) {
            throw new IllegalStateException("Step already completed");
        }
        if (status == ApprovalStatus.DEFERRED && target == ApprovalStatus.DEFERRED) {
            throw new IllegalStateException("Step already deferred");
        }
        this.status = target;
        this.actedBy = actor;
        this.actedAt = now;
    }
}
