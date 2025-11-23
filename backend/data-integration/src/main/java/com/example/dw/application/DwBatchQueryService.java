package com.example.dw.application;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;
import com.example.common.cache.CacheNames;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.repository.HrBatchRepository;

@Service
@RequiredArgsConstructor
public class DwBatchQueryService {

    private final HrBatchRepository batchRepository;
    private final AuditPort auditPort;

    public Page<HrImportBatchEntity> getBatches(Pageable pageable) {
        Page<HrImportBatchEntity> page = batchRepository.findAll(pageable);
        audit("DW_BATCH_LIST", "page=" + pageable.getPageNumber(), page.getTotalElements(), true);
        return page;
    }

    @Cacheable(cacheNames = CacheNames.LATEST_DW_BATCH, key = "'singleton'", sync = true)
    public Optional<HrImportBatchEntity> latestBatch() {
        Optional<HrImportBatchEntity> latest = batchRepository.findLatest();
        audit("DW_BATCH_LATEST", "latest", latest.isPresent() ? 1 : 0, latest.isPresent());
        return latest;
    }

    private void audit(String action, String subjectKey, long count, boolean success) {
        var actor = Actor.builder()
                .id("dw-ingestion")
                .type(ActorType.SYSTEM)
                .build();

        AuditEvent event = AuditEvent.builder()
                .eventType("DW_BATCH")
                .moduleName("data-integration")
                .action(action)
                .actor(actor)
                .subject(Subject.builder().type("DW_BATCH").key(subjectKey).build())
                .success(success)
                .resultCode(success ? "OK" : "NOT_FOUND")
                .riskLevel(RiskLevel.LOW)
                .extraEntry("count", count)
                .build();
        try {
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (Exception ignore) {
            // 감사 실패가 업무 흐름을 막지 않도록 삼킴
        }
    }
}
