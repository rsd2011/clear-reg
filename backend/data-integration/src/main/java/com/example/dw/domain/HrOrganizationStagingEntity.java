package com.example.dw.domain;

import java.time.LocalDate;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dw_org_staging",
        indexes = {
                @Index(name = "idx_dw_org_staging_batch_code", columnList = "batch_id, organization_code", unique = true)
        })
public class HrOrganizationStagingEntity extends PrimaryKeyEntity {

    protected HrOrganizationStagingEntity() {
    }

    private HrOrganizationStagingEntity(HrImportBatchEntity batch,
                                        String organizationCode,
                                        String name,
                                        String parentOrganizationCode,
                                        String status,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        String payloadHash,
                                        String rawPayload) {
        this.batch = batch;
        this.organizationCode = organizationCode;
        this.name = name;
        this.parentOrganizationCode = parentOrganizationCode;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payloadHash = payloadHash;
        this.rawPayload = rawPayload;
    }

    public static HrOrganizationStagingEntity fromRecord(HrImportBatchEntity batch,
                                                         com.example.dw.dto.HrOrganizationRecord record,
                                                         String payloadHash) {
        if (batch == null || record == null) {
            throw new IllegalArgumentException("batch and record are required");
        }
        return new HrOrganizationStagingEntity(
                batch,
                record.organizationCode(),
                record.name(),
                record.parentOrganizationCode(),
                record.status(),
                record.startDate(),
                record.endDate(),
                payloadHash,
                record.rawPayload()
        );
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private HrImportBatchEntity batch;

    @Column(name = "organization_code", nullable = false, length = 64)
    private String organizationCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "parent_code", length = 64)
    private String parentOrganizationCode;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    public HrImportBatchEntity getBatch() {
        return batch;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public String getName() {
        return name;
    }

    public String getParentOrganizationCode() {
        return parentOrganizationCode;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public String getRawPayload() {
        return rawPayload;
    }
}
