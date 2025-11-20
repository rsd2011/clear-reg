package com.example.draft.domain;

public enum DraftStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }
}
