package com.example.dw.application.export;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;

/**
 * 대량 export/파일 생성 시 AuditPort로 감사 이벤트를 남기기 위한 공통 헬퍼.
 * 실제 export 기능이 추가될 때 주입 받아 호출만 하면 되도록 분리.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportAuditService {

    private final AuditPort auditPort;

    public void auditExport(String exportType,
                            long recordCount,
                            String reasonCode,
                            String reasonText,
                            String legalBasisCode,
                            boolean success,
                            Map<String, Object> extra) {
        AuditEvent.AuditEventBuilder builder = AuditEvent.builder()
                .eventType("EXPORT")
                .moduleName("data-integration")
                .action("EXPORT_" + exportType.toUpperCase())
                .actor(Actor.builder().id("dw-export").type(ActorType.SYSTEM).build())
                .subject(Subject.builder().type("EXPORT").key(UUID.randomUUID().toString()).build())
                .success(success)
                .resultCode(success ? "OK" : "FAIL")
                .riskLevel(RiskLevel.HIGH)
                .extraEntry("recordCount", recordCount);

        if (reasonCode != null) {
            builder.reasonCode(reasonCode);
            builder.extraEntry("reasonCode", reasonCode);
        }
        if (reasonText != null) {
            builder.reasonText(reasonText);
            builder.extraEntry("reasonText", reasonText);
        }
        if (legalBasisCode != null) {
            builder.legalBasisCode(legalBasisCode);
            builder.extraEntry("legalBasisCode", legalBasisCode);
        }

        if (extra != null) {
            extra.forEach(builder::extraEntry);
        }
        try {
            auditPort.record(builder.build(), AuditMode.ASYNC_FALLBACK);
        } catch (Exception e) {
            log.warn("Export audit logging skipped: {}", e.getMessage());
        }
    }
}
