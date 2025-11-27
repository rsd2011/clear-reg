package com.example.approval.domain;

import com.example.approval.api.ApprovalStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ApprovalRequest {
    private final UUID id;
    private final UUID draftId;
    private final String templateCode;
    private final String organizationCode;
    private final String requester;
    private final String summary;
    private final OffsetDateTime createdAt;
    private OffsetDateTime lastUpdatedAt;
    private ApprovalStatus status;
    private final List<ApprovalStep> steps;
    private boolean approvalHookDispatched;

    private ApprovalRequest(UUID id,
                            UUID draftId,
                            String templateCode,
                            String organizationCode,
                            String requester,
                            String summary,
                            OffsetDateTime createdAt,
                            List<ApprovalStep> steps) {
        this.id = id;
        this.draftId = draftId;
        this.templateCode = templateCode;
        this.organizationCode = organizationCode;
        this.requester = requester;
        this.summary = summary;
        this.createdAt = createdAt;
        this.lastUpdatedAt = createdAt;
        this.status = ApprovalStatus.REQUESTED;
        this.steps = steps;
    }

    public static ApprovalRequest create(UUID draftId,
                                         String templateCode,
                                         String organizationCode,
                                         String requester,
                                         String summary,
                                         List<ApprovalStep> steps,
                                         OffsetDateTime now) {
        return new ApprovalRequest(UUID.randomUUID(), draftId, templateCode, organizationCode, requester, summary, now,
                new ArrayList<>(steps));
    }

    public UUID getId() {
        return id;
    }

    public UUID getDraftId() {
        return draftId;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public List<ApprovalStep> getSteps() {
        return steps;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void approve(String actor, OffsetDateTime now) {
        actOnCurrentStep(actor, now, ApprovalStatus.APPROVED);
    }

    public void reject(String actor, OffsetDateTime now) {
        actOnCurrentStep(actor, now, ApprovalStatus.REJECTED);
    }

    public void defer(String actor, OffsetDateTime now) {
        actOnCurrentStep(actor, now, ApprovalStatus.DEFERRED);
    }

    public void approveDeferred(String actor, OffsetDateTime now) {
        ApprovalStep deferred = steps.stream()
                .filter(step -> step.getStatus() == ApprovalStatus.DEFERRED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No deferred step"));
        deferred.completeDeferred(actor, now);
        recomputeStatus(now, actor);
    }

    public void delegate(String delegatedTo, String actor, String comment, OffsetDateTime now) {
        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED || status == ApprovalStatus.WITHDRAWN) {
            throw new IllegalStateException("Approval already completed");
        }
        ApprovalStep waiting = steps.stream()
                .filter(step -> step.getStatus() == ApprovalStatus.REQUESTED || step.getStatus() == ApprovalStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active step"));

        waiting.delegateTo(delegatedTo, actor, comment, now);
        lastUpdatedAt = now;
    }

    public void withdraw(String actor, OffsetDateTime now) {
        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED || status == ApprovalStatus.APPROVED_WITH_DEFER) {
            throw new IllegalStateException("Cannot withdraw after completion");
        }
        this.status = ApprovalStatus.WITHDRAWN;
        this.lastUpdatedAt = now;
    }

    private void actOnCurrentStep(String actor, OffsetDateTime now, ApprovalStatus target) {
        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED || status == ApprovalStatus.WITHDRAWN) {
            throw new IllegalStateException("Approval already completed");
        }

        ApprovalStep waiting = steps.stream()
                .filter(step -> step.getStatus() == ApprovalStatus.REQUESTED || step.getStatus() == ApprovalStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active step"));

        switch (target) {
            case APPROVED -> waiting.approve(actor, now);
            case REJECTED -> waiting.reject(actor, now);
            case DEFERRED -> waiting.defer(actor, now);
            default -> throw new IllegalArgumentException("Unsupported target: " + target);
        }
        recomputeStatus(now, actor);
    }

    private void recomputeStatus(OffsetDateTime now, String actor) {
        boolean anyRejected = steps.stream().anyMatch(step -> step.getStatus() == ApprovalStatus.REJECTED);
        boolean allApproved = steps.stream().allMatch(step -> step.getStatus() == ApprovalStatus.APPROVED);
        boolean hasDeferred = steps.stream().anyMatch(step -> step.getStatus() == ApprovalStatus.DEFERRED);
        boolean anyRequested = steps.stream().anyMatch(step -> step.getStatus() == ApprovalStatus.REQUESTED || step.getStatus() == ApprovalStatus.IN_PROGRESS);

        if (anyRejected) {
            status = ApprovalStatus.REJECTED;
        } else if (allApproved) {
            status = ApprovalStatus.APPROVED;
        } else if (!anyRequested && hasDeferred) {
            status = ApprovalStatus.APPROVED_WITH_DEFER;
        } else if (hasDeferred) {
            status = ApprovalStatus.DEFERRED;
        } else {
            status = ApprovalStatus.IN_PROGRESS;
        }

        lastUpdatedAt = now;
    }
}
