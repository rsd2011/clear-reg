package com.example.draft.application.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftStatus;

public record DraftResponse(UUID id,
                            String title,
                            String content,
                            String businessFeatureCode,
                            String organizationCode,
                            String createdBy,
                            DraftStatus status,
                            String templateCode,
                            String formTemplateCode,
                            Integer formTemplateVersion,
                            String formTemplateSnapshot,
                            String formPayload,
                            OffsetDateTime createdAt,
                            OffsetDateTime updatedAt,
                            OffsetDateTime submittedAt,
                            OffsetDateTime completedAt,
                            OffsetDateTime cancelledAt,
                            OffsetDateTime withdrawnAt,
                            List<DraftApprovalStepResponse> approvalSteps,
                             List<DraftAttachmentResponse> attachments) {

    public static DraftResponse from(Draft draft) {
        List<DraftApprovalStepResponse> steps = draft.getApprovalSteps().stream()
                .map(DraftApprovalStepResponse::from)
                .toList();
        List<DraftAttachmentResponse> files = draft.getAttachments().stream()
                .map(DraftAttachmentResponse::from)
                .toList();
        return new DraftResponse(
                draft.getId(),
                draft.getTitle(),
                draft.getContent(),
                draft.getBusinessFeatureCode(),
                draft.getOrganizationCode(),
                draft.getCreatedBy(),
                draft.getStatus(),
                draft.getTemplateCode(),
                draft.getFormTemplateCode(),
                draft.getFormTemplateVersion(),
                draft.getFormTemplateSnapshot(),
                draft.getFormPayload(),
                draft.getCreatedAt(),
                draft.getUpdatedAt(),
                draft.getSubmittedAt(),
                draft.getCompletedAt(),
                draft.getCancelledAt(),
                draft.getWithdrawnAt(),
                steps,
                files);
    }
}
