package com.example.approval.api;

import com.example.common.codegroup.annotation.ManagedCode;

@ManagedCode
public enum ApprovalStatus {
    REQUESTED,
    IN_PROGRESS,
    APPROVED,
    REJECTED,
    DEFERRED,
    APPROVED_WITH_DEFER,
    WITHDRAWN
}
