package com.example.common.policy;

/** 공통 정책 변경 이벤트. code 예: "security.policy" */
public record PolicyChangedEvent(String code, String payload) {
}
