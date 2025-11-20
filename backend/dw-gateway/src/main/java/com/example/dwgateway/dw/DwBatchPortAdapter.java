package com.example.dwgateway.dw;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import com.example.dw.application.DwBatchQueryService;
import com.example.dw.domain.HrImportBatchEntity;

@Component
public class DwBatchPortAdapter implements DwBatchPort {

    private final DwBatchQueryService batchQueryService;

    public DwBatchPortAdapter(DwBatchQueryService batchQueryService) {
        this.batchQueryService = batchQueryService;
    }

    @Override
    public Page<DwBatchRecord> getBatches(Pageable pageable) {
        Page<HrImportBatchEntity> entities = batchQueryService.getBatches(pageable);
        return new PageImpl<>(entities.getContent().stream().map(this::toRecord).toList(),
                pageable, entities.getTotalElements());
    }

    @Override
    public Optional<DwBatchRecord> latestBatch() {
        return batchQueryService.latestBatch().map(this::toRecord);
    }

    private DwBatchRecord toRecord(HrImportBatchEntity entity) {
        return new DwBatchRecord(entity.getId(), entity.getFileName(), entity.getFeedType(),
                entity.getSourceName(), entity.getBusinessDate(), entity.getStatus(),
                entity.getTotalRecords(), entity.getInsertedRecords(), entity.getUpdatedRecords(),
                entity.getFailedRecords(), entity.getReceivedAt(), entity.getCompletedAt(), entity.getErrorMessage());
    }
}
