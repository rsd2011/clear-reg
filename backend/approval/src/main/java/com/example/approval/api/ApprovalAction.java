package com.example.approval.api;

import com.example.common.codegroup.annotation.ManagedCode;

@ManagedCode
public enum ApprovalAction {
    APPROVE,
    REJECT,
    DEFER,
    DEFER_APPROVE,
    WITHDRAW,
    DELEGATE
}
