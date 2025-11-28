package com.example.dw.application.job;

import com.example.common.codegroup.annotation.ManagedCode;

@ManagedCode
public enum DwIngestionOutboxStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
    DEAD_LETTER
}
