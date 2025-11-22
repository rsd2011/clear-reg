package com.example.audit.infra.persistence;

import java.time.Instant;
import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity extends PrimaryKeyEntity {

    @Column(nullable = false)
    private Instant eventTime;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(name = "event_id", columnDefinition = "uuid", nullable = false, updatable = false)
    private java.util.UUID eventId;

    @Column(length = 32)
    private String moduleName;

    @Column(length = 64)
    private String action;

    @Column(length = 64)
    private String actorId;

    @Column(length = 16)
    private String actorType;

    @Column(length = 64)
    private String actorRole;

    @Column(length = 64)
    private String actorDept;

    @Column(length = 32)
    private String subjectType;

    @Column(length = 128)
    private String subjectKey;

    @Column(length = 32)
    private String channel;

    @Column(length = 64)
    private String clientIp;

    @Column(length = 256)
    private String userAgent;

    @Column(length = 128)
    private String deviceId;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 32)
    private String resultCode;

    @Column(length = 32)
    private String reasonCode;

    @Column(length = 512)
    private String reasonText;

    @Column(length = 32)
    private String legalBasisCode;

    @Column(length = 8)
    private String riskLevel;

    @Column(length = 1024)
    private String beforeSummary;

    @Column(length = 1024)
    private String afterSummary;

    @Column(columnDefinition = "text")
    private String extraJson;

    @Column(length = 128)
    private String hashChain;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(java.util.UUID eventId,
                          Instant eventTime,
                          String eventType,
                          String moduleName,
                          String action,
                          String actorId,
                          String actorType,
                          String actorRole,
                          String actorDept,
                          String subjectType,
                          String subjectKey,
                          String channel,
                          String clientIp,
                          String userAgent,
                          String deviceId,
                          boolean success,
                          String resultCode,
                          String reasonCode,
                          String reasonText,
                          String legalBasisCode,
                          String riskLevel,
                          String beforeSummary,
                          String afterSummary,
                          String extraJson,
                          String hashChain) {
        this.eventId = java.util.UUID.randomUUID();
        this.eventId = eventId == null ? java.util.UUID.randomUUID() : eventId;
        this.eventTime = eventTime;
        this.eventType = eventType;
        this.moduleName = moduleName;
        this.action = action;
        this.actorId = actorId;
        this.actorType = actorType;
        this.actorRole = actorRole;
        this.actorDept = actorDept;
        this.subjectType = subjectType;
        this.subjectKey = subjectKey;
        this.channel = channel;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.deviceId = deviceId;
        this.success = success;
        this.resultCode = resultCode;
        this.reasonCode = reasonCode;
        this.reasonText = reasonText;
        this.legalBasisCode = legalBasisCode;
        this.riskLevel = riskLevel;
        this.beforeSummary = beforeSummary;
        this.afterSummary = afterSummary;
        this.extraJson = extraJson;
        this.hashChain = hashChain;
    }
}
