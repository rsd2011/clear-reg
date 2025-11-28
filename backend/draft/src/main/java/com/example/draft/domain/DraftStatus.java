package com.example.draft.domain;

import com.example.common.codegroup.annotation.ManagedCode;

@ManagedCode
public enum DraftStatus {
    DRAFT,
    IN_REVIEW,
    APPROVED,
    APPROVED_WITH_DEFER,
    REJECTED,
    CANCELLED,
    WITHDRAWN;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED || this == WITHDRAWN;
    }
}
