package com.example.dw.domain;

import com.example.common.codegroup.annotation.ManagedCode;

@ManagedCode
public enum HrBatchStatus {
    RECEIVED,
    VALIDATED,
    COMPLETED,
    FAILED
}
