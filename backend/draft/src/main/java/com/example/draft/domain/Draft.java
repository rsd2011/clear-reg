package com.example.draft.domain;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.draft.domain.exception.DraftAccessDeniedException;
import com.example.draft.domain.exception.DraftWorkflowException;
import com.example.approval.api.ApprovalStatus;
import com.example.draft.domain.DraftFormTemplate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drafts",
        indexes = {
                @Index(name = "idx_draft_business_org", columnList = "business_feature_code, organization_code"),
                @Index(name = "idx_draft_status_org", columnList = "status, organization_code")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Draft extends PrimaryKeyEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "business_feature_code", nullable = false, length = 100)
    private String businessFeatureCode;

    @Column(name = "organization_code", nullable = false, length = 64)
    private String organizationCode;

    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode;

    @Column(name = "template_preset_id")
    private UUID templatePresetId;

    @Column(name = "form_template_code", length = 100)
    private String formTemplateCode;

    @Column(name = "form_template_version")
    private Integer formTemplateVersion;

    @Column(name = "form_template_snapshot", columnDefinition = "text")
    private String formTemplateSnapshot;

    @Column(name = "form_payload", columnDefinition = "text")
    private String formPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private DraftStatus status = DraftStatus.DRAFT;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "withdrawn_at")
    private OffsetDateTime withdrawnAt;

    @Column(name = "approval_request_id")
    private UUID approvalRequestId;

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private final List<DraftApprovalStep> approvalSteps = new ArrayList<>();

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC")
    private final List<DraftHistory> history = new ArrayList<>();

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attachedAt ASC")
    private final List<DraftAttachment> attachments = new ArrayList<>();

    private Draft(String title,
                  String content,
                  String businessFeatureCode,
                  String organizationCode,
                  String templateCode,
                  String actor,
                  OffsetDateTime now) {
        this.title = title;
        this.content = content;
        this.businessFeatureCode = businessFeatureCode;
        this.organizationCode = organizationCode;
        this.templateCode = templateCode;
        this.createdBy = actor;
        this.updatedBy = actor;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void linkApprovalRequest(UUID approvalRequestId) {
        if (this.approvalRequestId != null) {
            return;
        }
        this.approvalRequestId = approvalRequestId;
    }

    public void applyApprovalResult(ApprovalStatus status, String actor, String comment, OffsetDateTime now) {
        if (status == ApprovalStatus.APPROVED) {
            this.status = DraftStatus.APPROVED;
            this.completedAt = now;
        } else if (status == ApprovalStatus.APPROVED_WITH_DEFER) {
            this.status = DraftStatus.APPROVED_WITH_DEFER;
            this.completedAt = now;
        } else if (status == ApprovalStatus.REJECTED) {
            this.status = DraftStatus.REJECTED;
        } else if (status == ApprovalStatus.WITHDRAWN) {
            this.status = DraftStatus.WITHDRAWN;
        }
        this.updatedBy = actor;
        this.updatedAt = now;
    }

    public static Draft create(String title,
                               String content,
                               String businessFeatureCode,
                               String organizationCode,
                               String templateCode,
                               String actor,
                               OffsetDateTime now) {
        return new Draft(title, content, businessFeatureCode, organizationCode, templateCode, actor, now);
    }

    public void useTemplatePreset(UUID templatePresetId) {
        this.templatePresetId = templatePresetId;
    }

    public void attachFormTemplate(DraftFormTemplate template, String payload) {
        this.formTemplateCode = template.getTemplateCode();
        this.formTemplateVersion = template.getVersion();
        this.formTemplateSnapshot = template.getSchemaJson();
        this.formPayload = payload;
    }

    public void addApprovalStep(DraftApprovalStep step) {
        step.attachTo(this);
        this.approvalSteps.add(step);
    }

    public void initializeWorkflow(OffsetDateTime now) {
        this.history.add(DraftHistory.entry(this, "CREATED", this.createdBy, "기안 작성", now));
    }

    public void addAttachment(DraftAttachment attachment) {
        attachment.attachTo(this);
        this.attachments.add(attachment);
    }

    public void submit(String actor, OffsetDateTime now) {
        requireStatus(DraftStatus.DRAFT);
        this.status = DraftStatus.IN_REVIEW;
        this.submittedAt = now;
        this.updatedAt = now;
        this.updatedBy = actor;
        DraftApprovalStep waiting = currentWaitingStep()
                .orElseThrow(() -> new DraftWorkflowException("결재선이 설정되지 않았습니다."));
        waiting.start(now);
        this.history.add(DraftHistory.entry(this, "SUBMITTED", actor, "기안 상신", now));
    }

    public void approveStep(UUID stepId, String actor, String comment, OffsetDateTime now) {
        DraftApprovalStep step = findStep(stepId);
        ensureWritable();
        step.approve(actor, comment, now);
        this.updatedAt = now;
        this.updatedBy = actor;
        this.history.add(DraftHistory.entry(this, "APPROVED_STEP", actor,
                "결재 그룹 %s 승인".formatted(step.getApprovalGroupCode()), now));
        moveToNextStep(now, actor);
    }

    public void rejectStep(UUID stepId, String actor, String comment, OffsetDateTime now) {
        DraftApprovalStep step = findStep(stepId);
        ensureWritable();
        step.reject(actor, comment, now);
        this.status = DraftStatus.REJECTED;
        this.completedAt = now;
        this.updatedAt = now;
        this.updatedBy = actor;
        this.history.add(DraftHistory.entry(this, "REJECTED", actor, comment, now));
        skipRemainingSteps(now, "결재 반려로 인한 스킵");
    }

    public void deferStep(UUID stepId, String actor, String comment, OffsetDateTime now) {
        DraftApprovalStep step = findStep(stepId);
        ensureWritable();
        step.defer(actor, comment, now);
        this.updatedAt = now;
        this.updatedBy = actor;
        this.history.add(DraftHistory.entry(this, "DEFERRED", actor, "후결재 요청: " + step.getApprovalGroupCode(), now));
        moveToNextStep(now, actor);
    }

    public void approveDeferredStep(UUID stepId, String actor, String comment, OffsetDateTime now) {
        DraftApprovalStep step = findStep(stepId);
        if (step.getState() != DraftApprovalState.DEFERRED) {
            throw new DraftWorkflowException("후결재 대상 단계가 아닙니다.");
        }
        step.completeDeferred(actor, comment, now);
        this.updatedAt = now;
        this.updatedBy = actor;
        this.history.add(DraftHistory.entry(this, "DEFER_APPROVED", actor, comment, now));
        finalizeIfCompleted(now, actor);
    }

    public void cancel(String actor, OffsetDateTime now) {
        if (this.status.isTerminal()) {
            throw new DraftWorkflowException("이미 종료된 기안입니다.");
        }
        this.status = DraftStatus.CANCELLED;
        this.cancelledAt = now;
        this.updatedAt = now;
        this.updatedBy = actor;
        this.history.add(DraftHistory.entry(this, "CANCELLED", actor, null, now));
        skipRemainingSteps(now, "기안 취소");
    }

    public void withdraw(String actor, OffsetDateTime now) {
        if (this.status.isTerminal()) {
            throw new DraftWorkflowException("이미 종료된 기안입니다.");
        }
        if (this.status == DraftStatus.APPROVED_WITH_DEFER) {
            throw new DraftWorkflowException("후결재 대기 상태에서는 회수할 수 없습니다.");
        }
        this.status = DraftStatus.WITHDRAWN;
        this.withdrawnAt = now;
        this.updatedAt = now;
        this.updatedBy = actor;
        this.history.add(DraftHistory.entry(this, "WITHDRAWN", actor, "기안 회수", now));
        skipRemainingSteps(now, "기안 회수");
    }

    public void resubmit(String actor, OffsetDateTime now) {
        if (this.status != DraftStatus.WITHDRAWN) {
            throw new DraftWorkflowException("회수된 기안만 재상신할 수 있습니다.");
        }
        resetSteps();
        this.status = DraftStatus.IN_REVIEW;
        this.submittedAt = now;
        this.completedAt = null;
        this.cancelledAt = null;
        this.withdrawnAt = null;
        this.updatedAt = now;
        this.updatedBy = actor;
        history.add(DraftHistory.entry(this, "RESUBMITTED", actor, "기안 재상신", now));
        DraftApprovalStep waiting = currentWaitingStep()
                .orElseThrow(() -> new DraftWorkflowException("결재선이 설정되지 않았습니다."));
        waiting.start(now);
    }

    public void delegate(UUID stepId, String delegatedTo, String actor, String comment, OffsetDateTime now) {
        ensureWritable();
        DraftApprovalStep step = findStep(stepId);
        step.delegateTo(delegatedTo, comment, now);
        this.updatedAt = now;
        this.updatedBy = actor;
        this.history.add(DraftHistory.entry(this, "DELEGATED", actor, "결재 위임 대상: " + delegatedTo, now));
    }

    public DraftApprovalStep findStep(UUID stepId) {
        return approvalSteps.stream()
                .filter(step -> step.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new DraftWorkflowException("결재 단계를 찾을 수 없습니다."));
    }

    public boolean belongsToOrganization(String organizationCode) {
        return this.organizationCode.equals(organizationCode);
    }

    public void assertOrganizationAccess(String organizationCode, boolean auditAccess) {
        if (!belongsToOrganization(organizationCode) && !auditAccess) {
            throw new DraftAccessDeniedException("다른 조직의 기안에 접근할 수 없습니다.");
        }
    }

    private void moveToNextStep(OffsetDateTime now, String actor) {
        DraftApprovalStep next = approvalSteps.stream()
                .filter(step -> step.getState() == DraftApprovalState.WAITING)
                .findFirst()
                .orElse(null);
        if (next == null) {
            finalizeIfCompleted(now, actor);
        } else {
            next.start(now);
        }
    }

    private void finalizeIfCompleted(OffsetDateTime now, String actor) {
        boolean anyWaiting = approvalSteps.stream().anyMatch(step -> step.getState() == DraftApprovalState.WAITING || step.getState() == DraftApprovalState.IN_PROGRESS);
        boolean anyDeferred = approvalSteps.stream().anyMatch(step -> step.getState() == DraftApprovalState.DEFERRED);
        boolean allApprovedOrDeferred = approvalSteps.stream().allMatch(step -> step.getState() == DraftApprovalState.APPROVED || step.getState() == DraftApprovalState.DEFERRED || step.getState() == DraftApprovalState.SKIPPED);

        if (!anyWaiting && allApprovedOrDeferred) {
            if (anyDeferred) {
                this.status = DraftStatus.APPROVED_WITH_DEFER;
                this.completedAt = now;
                this.history.add(DraftHistory.entry(this, "COMPLETED_WITH_DEFER", actor, "모든 결재 완료(후결재 대기)", now));
            } else {
                this.status = DraftStatus.APPROVED;
                this.completedAt = now;
                this.history.add(DraftHistory.entry(this, "COMPLETED", actor, "모든 결재 완료", now));
            }
        }
    }

    private void skipRemainingSteps(OffsetDateTime now, String reason) {
        approvalSteps.stream()
                .filter(step -> !step.getState().isCompleted())
                .forEach(step -> step.skip(reason, now));
    }

    private void resetSteps() {
        approvalSteps.forEach(DraftApprovalStep::reset);
    }

    private void ensureWritable() {
        if (this.status.isTerminal()) {
            throw new DraftWorkflowException("이미 종료된 기안입니다.");
        }
        if (this.status == DraftStatus.DRAFT) {
            throw new DraftWorkflowException("상신되지 않은 기안입니다.");
        }
    }

    private void requireStatus(DraftStatus expected) {
        if (this.status != expected) {
            throw new DraftWorkflowException("현재 상태에서는 수행할 수 없습니다: " + this.status);
        }
    }

    private java.util.Optional<DraftApprovalStep> currentWaitingStep() {
        return approvalSteps.stream()
                .filter(step -> step.getState() == DraftApprovalState.WAITING)
                .findFirst();
    }

    public List<DraftAttachment> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }
}
