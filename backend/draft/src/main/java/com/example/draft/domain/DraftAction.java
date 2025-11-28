package com.example.draft.domain;

import com.example.common.codegroup.annotation.ManagedCode;

@ManagedCode
public enum DraftAction {
    CREATED,
    SUBMITTED,
    APPROVED,
    APPROVED_WITH_DEFER,
    REJECTED,
    CANCELLED,
    WITHDRAWN,
    RESUBMITTED,
    DELEGATED,
    DEFERRED,
    DEFER_APPROVED
}
