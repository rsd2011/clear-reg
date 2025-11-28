package com.example.file;

import com.example.common.codegroup.annotation.ManagedCode;

@ManagedCode
public enum ScanStatus {
    PENDING,
    CLEAN,
    BLOCKED,
    FAILED
}
