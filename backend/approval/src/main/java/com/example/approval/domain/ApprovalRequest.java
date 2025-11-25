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

    private void actOnCurrentStep(String actor, OffsetDateTime now, ApprovalStatus target) {
        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED) {
            throw new IllegalStateException("Approval already completed");
        }

        ApprovalStep waiting = steps.stream()
                .filter(step -> step.getStatus() == ApprovalStatus.REQUESTED || step.getStatus() == ApprovalStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active step"));

        if (target == ApprovalStatus.APPROVED) {
            waiting.approve(actor, now);
        } else {
            waiting.reject(actor, now);
        }

        boolean allApproved = steps.stream().allMatch(step -> step.getStatus() == ApprovalStatus.APPROVED);
        boolean anyRejected = steps.stream().anyMatch(step -> step.getStatus() == ApprovalStatus.REJECTED);

        if (anyRejected) {
            status = ApprovalStatus.REJECTED;
        } else if (allApproved) {
            status = ApprovalStatus.APPROVED;
        } else {
            status = ApprovalStatus.IN_PROGRESS;
        }

        lastUpdatedAt = now;
    }
}
