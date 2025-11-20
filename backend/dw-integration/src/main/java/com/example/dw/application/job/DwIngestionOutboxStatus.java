package com.example.dw.application.job;

public enum DwIngestionOutboxStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
    DEAD_LETTER
}
