package com.example.approval.api;

public record ApprovalActionCommand(
        ApprovalAction action,
        String actor,
        String comment
) {
    public ApprovalActionCommand {
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor is required");
        }
    }
}
