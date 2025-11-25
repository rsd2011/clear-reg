package com.example.approval.domain;

import com.example.approval.api.ApprovalStatus;

import java.time.OffsetDateTime;

public class ApprovalStep {
    private final int stepOrder;
    private final String approvalGroupCode;
    private ApprovalStatus status;
    private String actedBy;
    private OffsetDateTime actedAt;

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

    public void approve(String actor, OffsetDateTime now) {
        transitionTo(ApprovalStatus.APPROVED, actor, now);
    }

    public void reject(String actor, OffsetDateTime now) {
        transitionTo(ApprovalStatus.REJECTED, actor, now);
    }

    private void transitionTo(ApprovalStatus target, String actor, OffsetDateTime now) {
        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED) {
            throw new IllegalStateException("Step already completed");
        }
        this.status = target;
        this.actedBy = actor;
        this.actedAt = now;
    }
}
