package com.example.approval.api;

import java.util.UUID;

import java.util.List;

public record ApprovalRequestCommand(
        UUID draftId,
        String templateCode,
        String organizationCode,
        String requester,
        String summary,
        List<String> approvalGroupCodes
) {
    public ApprovalRequestCommand {
        if (draftId == null) {
            throw new IllegalArgumentException("draftId is required");
        }
        if (templateCode == null || templateCode.isBlank()) {
            throw new IllegalArgumentException("templateCode is required");
        }
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new IllegalArgumentException("organizationCode is required");
        }
        if (requester == null || requester.isBlank()) {
            throw new IllegalArgumentException("requester is required");
        }
        if (approvalGroupCodes == null || approvalGroupCodes.isEmpty()) {
            throw new IllegalArgumentException("at least one approval group is required");
        }
    }
}
