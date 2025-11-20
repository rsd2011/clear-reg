package com.example.dwgateway.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.dw.domain.HrBatchStatus;
import com.example.dw.dto.DataFeedType;
import com.example.dwgateway.dw.DwBatchPort;

public record DwBatchResponse(UUID id,
                              String fileName,
                              DataFeedType feedType,
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

    public static DwBatchResponse fromRecord(DwBatchPort.DwBatchRecord record) {
        return new DwBatchResponse(record.id(), record.fileName(), record.feedType(), record.sourceName(),
                record.businessDate(), record.status(),
                record.totalRecords(), record.insertedRecords(), record.updatedRecords(),
                record.failedRecords(), record.receivedAt(), record.completedAt(), record.errorMessage());
    }
}
