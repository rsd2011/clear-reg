package com.example.dw.domain;

import com.example.common.codegroup.annotation.ManagedCode;

@ManagedCode
public enum HrExternalFeedStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
