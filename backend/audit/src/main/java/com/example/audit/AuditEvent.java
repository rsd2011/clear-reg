package com.example.audit;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;

/** 중앙 감사 이벤트 공통 스키마. */
@Value
@Builder(toBuilder = true)
public class AuditEvent {
    @Builder.Default
    UUID eventId = UUID.randomUUID();

    @Builder.Default
    Instant eventTime = Instant.now();

    String eventType;
    String moduleName;
    String action;
    Actor actor;
    Subject subject;
    String channel;
    String clientIp;
    String userAgent;
    String deviceId;

    @Builder.Default
    boolean success = true;

    String resultCode;
    String reasonCode;
    String reasonText;
    String legalBasisCode;

    @Builder.Default
    RiskLevel riskLevel = RiskLevel.MEDIUM;

    String beforeSummary;
    String afterSummary;

    @Getter(lombok.AccessLevel.NONE)
    @Singular("extraEntry")
    Map<String, Object> extra;

    String hashChain;

    /** 외부 변조 방지를 위해 불변 Map 반환 */
    public Map<String, Object> getExtra() {
        return extra == null ? Collections.emptyMap() : Collections.unmodifiableMap(extra);
    }
}
