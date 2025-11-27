package com.example.draft.domain;

public enum DraftApprovalState {
    WAITING,
    IN_PROGRESS,
    APPROVED,
    REJECTED,
    DEFERRED,
    SKIPPED;

    public boolean isCompleted() {
        return this == APPROVED || this == REJECTED || this == SKIPPED || this == DEFERRED;
    }
}
