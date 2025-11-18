package com.example.hr.domain;

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
@Table(name = "hr_organization_staging",
        indexes = {
                @Index(name = "idx_hr_org_staging_batch_code", columnList = "batch_id, organization_code", unique = true)
        })
public class HrOrganizationStagingEntity extends PrimaryKeyEntity {

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

    public void setBatch(HrImportBatchEntity batch) {
        this.batch = batch;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentOrganizationCode() {
        return parentOrganizationCode;
    }

    public void setParentOrganizationCode(String parentOrganizationCode) {
        this.parentOrganizationCode = parentOrganizationCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
