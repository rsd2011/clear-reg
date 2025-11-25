package com.example.approval.infra.memory;

import com.example.approval.api.ApprovalAction;
import com.example.approval.api.ApprovalActionCommand;
import com.example.approval.api.ApprovalFacade;
import com.example.approval.api.ApprovalRequestCommand;
import com.example.approval.api.ApprovalStatusSnapshot;
import com.example.approval.api.ApprovalStepSnapshot;
import com.example.approval.domain.ApprovalRequest;
import com.example.approval.domain.ApprovalStep;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryApprovalFacade implements ApprovalFacade {

    private final Map<UUID, ApprovalRequest> approvals = new ConcurrentHashMap<>();

    @Override
    public ApprovalStatusSnapshot requestApproval(ApprovalRequestCommand command) {
        OffsetDateTime now = OffsetDateTime.now();
        List<String> codes = command.approvalGroupCodes();
        List<ApprovalStep> steps = java.util.stream.IntStream.range(0, codes.size())
                .mapToObj(idx -> new ApprovalStep(idx + 1, codes.get(idx)))
                .collect(Collectors.toList());

        ApprovalRequest request = ApprovalRequest.create(
                command.draftId(),
                command.templateCode(),
                command.organizationCode(),
                command.requester(),
                command.summary(),
                steps,
                now
        );

        approvals.put(request.getId(), request);
        return toSnapshot(request);
    }

    @Override
    public ApprovalStatusSnapshot actOnApproval(UUID approvalRequestId, ApprovalActionCommand command) {
        ApprovalRequest request = approvals.get(approvalRequestId);
        if (request == null) {
            throw new IllegalArgumentException("approval request not found: " + approvalRequestId);
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (command.action() == ApprovalAction.APPROVE) {
            request.approve(command.actor(), now);
        } else {
            request.reject(command.actor(), now);
        }

        return toSnapshot(request);
    }

    @Override
    public ApprovalStatusSnapshot findByDraftId(UUID draftId) {
        Optional<ApprovalRequest> match = approvals.values().stream()
                .filter(ar -> ar.getDraftId().equals(draftId))
                .findFirst();
        return match.map(this::toSnapshot).orElse(null);
    }

    private ApprovalStatusSnapshot toSnapshot(ApprovalRequest request) {
        List<ApprovalStepSnapshot> steps = request.getSteps().stream()
                .map(step -> new ApprovalStepSnapshot(
                        step.getStepOrder(),
                        step.getApprovalGroupCode(),
                        step.getStatus(),
                        step.getActedBy(),
                        step.getActedAt()
                ))
                .toList();
        return new ApprovalStatusSnapshot(request.getId(), request.getDraftId(), request.getStatus(), steps);
    }
}
