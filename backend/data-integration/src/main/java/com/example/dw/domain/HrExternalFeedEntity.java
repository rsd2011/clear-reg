package com.example.dw.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.dw.dto.DataFeedType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "dw_source_feeds")
public class HrExternalFeedEntity extends PrimaryKeyEntity {

    protected HrExternalFeedEntity() {
    }

    private HrExternalFeedEntity(DataFeedType feedType,
                                 String payload,
                                 LocalDate businessDate,
                                 int sequenceNumber,
                                 String sourceSystem) {
        if (feedType == null || payload == null || businessDate == null || sourceSystem == null) {
            throw new IllegalArgumentException("feedType, payload, businessDate, sourceSystem는 필수입니다.");
        }
        this.feedType = feedType;
        this.payload = payload;
        this.businessDate = businessDate;
        this.sequenceNumber = sequenceNumber;
        this.sourceSystem = sourceSystem;
    }

    public static HrExternalFeedEntity receive(DataFeedType feedType,
                                               String payload,
                                               LocalDate businessDate,
                                               int sequenceNumber,
                                               String sourceSystem) {
        return new HrExternalFeedEntity(feedType, payload, businessDate, sequenceNumber, sourceSystem);
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "feed_type", nullable = false, length = 32)
    private DataFeedType feedType;

    @Column(name = "payload", columnDefinition = "text", nullable = false)
    private String payload;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private HrExternalFeedStatus status = HrExternalFeedStatus.PENDING;

    @Column(name = "source_system", length = 64)
    private String sourceSystem = "external-db";

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

    public DataFeedType getFeedType() {
        return feedType;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public HrExternalFeedStatus getStatus() {
        return status;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markProcessing() {
        this.status = HrExternalFeedStatus.PROCESSING;
        touch();
    }

    public void markCompleted() {
        this.status = HrExternalFeedStatus.COMPLETED;
        this.errorMessage = null;
        touch();
    }

    public void markFailed(String errorMessage) {
        this.status = HrExternalFeedStatus.FAILED;
        this.errorMessage = errorMessage;
        touch();
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
