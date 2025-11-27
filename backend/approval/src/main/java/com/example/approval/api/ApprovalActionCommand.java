package com.example.approval.api;

public record ApprovalActionCommand(
        ApprovalAction action,
        String actor,
        String organizationCode,
        String comment,
        String delegatedTo
) {
    public ApprovalActionCommand {
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor is required");
        }
        if (organizationCode == null || organizationCode.isBlank()) {
            throw new IllegalArgumentException("organizationCode is required");
        }
    }
}
