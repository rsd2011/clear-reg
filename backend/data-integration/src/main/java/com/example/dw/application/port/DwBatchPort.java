package com.example.dw.application.port;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.dw.domain.HrBatchStatus;
import com.example.dw.dto.DataFeedType;

/**
 * Port interface for querying DW ingestion batches.
 */
public interface DwBatchPort {

    Page<DwBatchRecord> getBatches(Pageable pageable);

    Optional<DwBatchRecord> latestBatch();

    record DwBatchRecord(UUID id,
                         String fileName,
                         DataFeedType feedType,
                         String sourceName,
                         LocalDate businessDate,
                         HrBatchStatus status,
                         int totalRecords,
                         int insertedRecords,
                         int updatedRecords,
                         int failedRecords,
                         OffsetDateTime receivedAt,
                         OffsetDateTime completedAt,
                         String errorMessage) { }
}
