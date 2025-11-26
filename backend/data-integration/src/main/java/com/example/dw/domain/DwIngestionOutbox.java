package com.example.dw.domain;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.Duration;

import org.springframework.util.StringUtils;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.dw.application.job.DwIngestionJobType;
import com.example.dw.application.job.DwIngestionOutboxStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "dw_ingestion_outbox",
        indexes = @Index(name = "idx_dw_ingestion_outbox_status_created", columnList = "status, created_at"))
public class DwIngestionOutbox extends PrimaryKeyEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private DwIngestionJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DwIngestionOutboxStatus status = DwIngestionOutboxStatus.PENDING;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "available_at", nullable = false)
    private OffsetDateTime availableAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected DwIngestionOutbox() {
    }

    public DwIngestionOutbox(DwIngestionJobType jobType, OffsetDateTime availableAt, OffsetDateTime createdAt) {
        this.jobType = jobType;
        this.availableAt = availableAt;
        this.createdAt = createdAt;
    }

    public static DwIngestionOutbox pending(DwIngestionJobType jobType, Clock clock) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return new DwIngestionOutbox(jobType, now, now);
    }

    public DwIngestionJobType getJobType() {
        return jobType;
    }

    public DwIngestionOutboxStatus getStatus() {
        return status;
    }

    public OffsetDateTime getAvailableAt() {
        return availableAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getPayload() {
        return payload;
    }

    public OffsetDateTime getLockedAt() {
        return lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public DwIngestionOutbox withPayload(String payload) {
        this.payload = payload;
        return this;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void markSending(Clock clock) {
        markSending(clock, null);
    }

    public void markSending(Clock clock, String locker) {
        this.status = DwIngestionOutboxStatus.SENDING;
        this.processedAt = OffsetDateTime.now(clock);
        this.lockedAt = this.processedAt;
        this.lockedBy = locker;
    }

    public void markSent(Clock clock) {
        this.status = DwIngestionOutboxStatus.SENT;
        this.processedAt = OffsetDateTime.now(clock);
        this.lastError = null;
    }

    public void markFailed(Clock clock) {
        markFailed(clock, null);
    }

    public void markFailed(Clock clock, String errorMessage) {
        this.status = DwIngestionOutboxStatus.FAILED;
        this.processedAt = OffsetDateTime.now(clock);
        this.lastError = truncated(errorMessage);
    }

    public void markRetry(Clock clock, Duration delay, String errorMessage) {
        this.status = DwIngestionOutboxStatus.PENDING;
        this.retryCount += 1;
        this.availableAt = OffsetDateTime.now(clock).plus(delay);
        this.processedAt = OffsetDateTime.now(clock);
        this.lastError = truncated(errorMessage);
    }

    public void markDeadLetter(Clock clock, String errorMessage) {
        this.status = DwIngestionOutboxStatus.DEAD_LETTER;
        this.retryCount += 1;
        this.processedAt = OffsetDateTime.now(clock);
        this.lastError = truncated(errorMessage);
    }

    private String truncated(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
