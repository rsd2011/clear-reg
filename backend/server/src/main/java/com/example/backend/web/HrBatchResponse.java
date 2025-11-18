package com.example.backend.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.hr.domain.HrBatchStatus;
import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.dto.HrFeedType;

public record HrBatchResponse(UUID id,
                              String fileName,
                              HrFeedType feedType,
                              String sourceName,
                              java.time.LocalDate businessDate,
                              HrBatchStatus status,
                              int totalRecords,
                              int insertedRecords,
                              int updatedRecords,
                              int failedRecords,
                              OffsetDateTime receivedAt,
                              OffsetDateTime completedAt,
                              String errorMessage) {

    public static HrBatchResponse fromEntity(HrImportBatchEntity entity) {
        return new HrBatchResponse(entity.getId(), entity.getFileName(), entity.getFeedType(), entity.getSourceName(),
                entity.getBusinessDate(), entity.getStatus(),
                entity.getTotalRecords(), entity.getInsertedRecords(), entity.getUpdatedRecords(),
                entity.getFailedRecords(), entity.getReceivedAt(), entity.getCompletedAt(), entity.getErrorMessage());
    }
}
