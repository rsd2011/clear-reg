package com.example.audit.drm;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.audit.Actor;

import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;

@Service
public class DrmAuditService {

    private final AuditPort auditPort;

    public DrmAuditService(AuditPort auditPort) {
        this.auditPort = auditPort;
    }

    public void record(DrmAuditEvent drm, AuditMode mode) {
        AuditEvent event = AuditEvent.builder()
                .eventTime(Instant.now())
                .eventType("DRM")
                .moduleName("audit")
                .action(drm.getEventType().name())
                .actor(Actor.builder()
                        .id(drm.getRequestorId())
                        .type(ActorType.HUMAN)
                        .role(drm.getApproverId())
                        .dept(drm.getOrganizationCode())
                        .build())
                .subject(Subject.builder()
                        .type("ASSET")
                        .key(drm.getAssetId())
                        .build())
                .reasonCode(drm.getReasonCode())
                .reasonText(drm.getReasonText())
                .riskLevel(RiskLevel.HIGH)
                .extraEntry("expiresAt", drm.getExpiresAt())
                .extraEntry("route", drm.getRoute())
                .extraEntry("tags", drm.getTags())
                .success(true)
                .resultCode("OK")
                .build();
        auditPort.record(event, mode);
    }
}
