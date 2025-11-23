package com.example.dwgateway.dw;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.dw.application.DwBatchQueryService;
import com.example.dw.domain.HrImportBatchEntity;

@Component
public class DwBatchPortAdapter implements DwBatchPort {

    private final DwBatchQueryService batchQueryService;
    private final AuditPort auditPort;

    public DwBatchPortAdapter(DwBatchQueryService batchQueryService, AuditPort auditPort) {
        this.batchQueryService = batchQueryService;
        this.auditPort = auditPort;
    }

    @Override
    public Page<DwBatchRecord> getBatches(Pageable pageable) {
        record("BATCH_LIST", null);
        Page<HrImportBatchEntity> entities = batchQueryService.getBatches(pageable);
        return new PageImpl<>(entities.getContent().stream().map(this::toRecord).toList(),
                pageable, entities.getTotalElements());
    }

    @Override
    public Optional<DwBatchRecord> latestBatch() {
        record("BATCH_LATEST", null);
        return batchQueryService.latestBatch().map(this::toRecord);
    }

    private DwBatchRecord toRecord(HrImportBatchEntity entity) {
        return new DwBatchRecord(entity.getId(), entity.getFileName(), entity.getFeedType(),
                entity.getSourceName(), entity.getBusinessDate(), entity.getStatus(),
                entity.getTotalRecords(), entity.getInsertedRecords(), entity.getUpdatedRecords(),
                entity.getFailedRecords(), entity.getReceivedAt(), entity.getCompletedAt(), entity.getErrorMessage());
    }

    private void record(String action, String detail) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .eventType("DW_BATCH")
                    .moduleName("dw-integration")
                    .action(action)
                    .success(true)
                    .resultCode("OK")
                    .riskLevel(RiskLevel.MEDIUM)
                    .extraEntry("detail", detail)
                    .build();
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (Exception ignore) {
            // 감사 실패로 업무 흐름 막지 않음
        }
    }
}
