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
@Table(name = "dw_employee_staging",
        indexes = {
                @Index(name = "idx_dw_staging_batch_employee", columnList = "batch_id, employee_id", unique = true)
        })
public class HrEmployeeStagingEntity extends PrimaryKeyEntity {

    protected HrEmployeeStagingEntity() {
    }

    private HrEmployeeStagingEntity(HrImportBatchEntity batch,
                                    String employeeId,
                                    String fullName,
                                    String email,
                                    String organizationCode,
                                    String employmentType,
                                    String employmentStatus,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    String payloadHash,
                                    String rawPayload) {
        this.batch = batch;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.email = email;
        this.organizationCode = organizationCode;
        this.employmentType = employmentType;
        this.employmentStatus = employmentStatus;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payloadHash = payloadHash;
        this.rawPayload = rawPayload;
    }

    public static HrEmployeeStagingEntity fromRecord(HrImportBatchEntity batch,
                                                     com.example.dw.dto.HrEmployeeRecord record,
                                                     String payloadHash) {
        if (batch == null || record == null) {
            throw new IllegalArgumentException("batch and record are required");
        }
        return new HrEmployeeStagingEntity(
                batch,
                record.employeeId(),
                record.fullName(),
                record.email(),
                record.organizationCode(),
                record.employmentType(),
                record.employmentStatus(),
                record.startDate(),
                record.endDate(),
                payloadHash,
                record.rawPayload()
        );
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private HrImportBatchEntity batch;

    @Column(name = "employee_id", nullable = false, length = 64)
    private String employeeId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "organization_code", length = 64)
    private String organizationCode;

    @Column(name = "employment_type", length = 64)
    private String employmentType;

    @Column(name = "employment_status", length = 32)
    private String employmentStatus;

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

    public String getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
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
