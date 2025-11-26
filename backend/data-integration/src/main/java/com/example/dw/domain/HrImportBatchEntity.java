package com.example.dw.domain;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.dw.dto.DataFeedType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "dw_import_batches",
        indexes = {
                @Index(name = "idx_dw_import_batch_file", columnList = "business_date, sequence_number", unique = true)
        })
public class HrImportBatchEntity extends PrimaryKeyEntity {

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "feed_type", nullable = false, length = 32)
    private DataFeedType feedType = DataFeedType.EMPLOYEE;

    @Column(name = "source_name", length = 64)
    private String sourceName;

    @Column(name = "business_date", nullable = false)
    private java.time.LocalDate businessDate;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "checksum", nullable = false, length = 128)
    private String checksum;

    @Column(name = "source_path", length = 512)
    private String sourcePath;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HrBatchStatus status = HrBatchStatus.RECEIVED;

    @Column(name = "records_total")
    private int totalRecords;

    @Column(name = "records_inserted")
    private int insertedRecords;

    @Column(name = "records_updated")
    private int updatedRecords;

    @Column(name = "records_failed")
    private int failedRecords;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @PrePersist
    void setReceivedAtIfNeeded() {
        if (receivedAt == null) {
            receivedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public String getFileName() { return fileName; }
    public DataFeedType getFeedType() { return feedType; }
    public String getSourceName() { return sourceName; }
    public java.time.LocalDate getBusinessDate() { return businessDate; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getChecksum() { return checksum; }
    public String getSourcePath() { return sourcePath; }
    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public HrBatchStatus getStatus() { return status; }
    public int getTotalRecords() { return totalRecords; }
    public int getInsertedRecords() { return insertedRecords; }
    public int getUpdatedRecords() { return updatedRecords; }
    public int getFailedRecords() { return failedRecords; }
    public String getErrorMessage() { return errorMessage; }

    public static HrImportBatchEntity receive(String fileName,
                                              DataFeedType feedType,
                                              String sourceName,
                                              java.time.LocalDate businessDate,
                                              int sequenceNumber,
                                              String checksum,
                                              String sourcePath) {
        HrImportBatchEntity batch = new HrImportBatchEntity();
        batch.fileName = fileName;
        batch.feedType = feedType;
        batch.sourceName = sourceName;
        batch.businessDate = businessDate;
        batch.sequenceNumber = sequenceNumber;
        batch.checksum = checksum;
        batch.sourcePath = sourcePath;
        batch.receivedAt = OffsetDateTime.now(ZoneOffset.UTC);
        batch.status = HrBatchStatus.RECEIVED;
        return batch;
    }

    public void markValidated(int totalRecords, int failedRecords) {
        this.status = HrBatchStatus.VALIDATED;
        this.totalRecords = totalRecords;
        this.failedRecords = failedRecords;
    }

    public void markCompleted(int insertedRecords, int updatedRecords, int failedRecords) {
        this.status = HrBatchStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.insertedRecords = insertedRecords;
        this.updatedRecords = updatedRecords;
        this.failedRecords = failedRecords;
    }

    public void markFailed(String errorMessage) {
        this.status = HrBatchStatus.FAILED;
        this.completedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.errorMessage = errorMessage;
    }

    public boolean isTerminal() {
        return status == HrBatchStatus.COMPLETED || status == HrBatchStatus.FAILED;
    }
}
