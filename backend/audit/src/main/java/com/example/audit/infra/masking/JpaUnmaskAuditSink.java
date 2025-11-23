package com.example.audit.infra.masking;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.masking.UnmaskAuditEvent;
import com.example.common.masking.UnmaskAuditSink;

@Component
public class JpaUnmaskAuditSink implements UnmaskAuditSink {

    private final UnmaskAuditRepository repository;

    public JpaUnmaskAuditSink(UnmaskAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void handle(UnmaskAuditEvent event) {
        UnmaskAuditRecord record = UnmaskAuditRecord.builder()
                .eventTime(event.getEventTime() != null ? event.getEventTime() : Instant.now())
                .subjectType(event.getSubjectType())
                .dataKind(event.getDataKind())
                .fieldName(event.getFieldName())
                .rowId(event.getRowId())
                .requesterRoles(event.getRequesterRoles() == null ? null :
                        event.getRequesterRoles().stream().collect(Collectors.joining(",")))
                .reason(event.getReason())
                .build();
        repository.save(record);
    }
}
