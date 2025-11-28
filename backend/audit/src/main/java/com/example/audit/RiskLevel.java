package com.example.audit;

import com.example.common.codegroup.annotation.ManagedCode;

/** 감사 이벤트의 리스크 레벨. */
@ManagedCode
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
