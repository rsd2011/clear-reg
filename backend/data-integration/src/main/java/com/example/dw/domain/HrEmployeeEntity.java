package com.example.dw.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "dw_employees",
        indexes = {
                @Index(name = "idx_dw_employee_employee_id", columnList = "employee_id"),
                @Index(name = "idx_dw_employee_active", columnList = "employee_id, effective_end")
        })
public class HrEmployeeEntity extends PrimaryKeyEntity {

    protected HrEmployeeEntity() {
    }

    private HrEmployeeEntity(String employeeId,
                             int version,
                             String fullName,
                             String email,
                             String organizationCode,
                             String employmentType,
                             String employmentStatus,
                             LocalDate effectiveStart,
                             LocalDate effectiveEnd,
                             UUID sourceBatchId,
                             OffsetDateTime syncedAt) {
        this.employeeId = employeeId;
        this.version = version;
        this.fullName = fullName;
        this.email = email;
        this.organizationCode = organizationCode;
        this.employmentType = employmentType;
        this.employmentStatus = employmentStatus;
        this.effectiveStart = effectiveStart;
        this.effectiveEnd = effectiveEnd;
        this.sourceBatchId = sourceBatchId;
        this.syncedAt = syncedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : syncedAt;
    }

    public static HrEmployeeEntity snapshot(String employeeId,
                                            int version,
                                            String fullName,
                                            String email,
                                            String organizationCode,
                                            String employmentType,
                                            String employmentStatus,
                                            LocalDate effectiveStart,
                                            LocalDate effectiveEnd,
                                            UUID sourceBatchId,
                                            OffsetDateTime syncedAt) {
        return new HrEmployeeEntity(employeeId, version, fullName, email, organizationCode,
                employmentType, employmentStatus, effectiveStart, effectiveEnd, sourceBatchId, syncedAt);
    }

    @Column(name = "employee_id", nullable = false, length = 64)
    private String employeeId;

    @Column(name = "version", nullable = false)
    private int version;

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

    @Column(name = "effective_start", nullable = false)
    private LocalDate effectiveStart;

    @Column(name = "effective_end")
    private LocalDate effectiveEnd;

    @Column(name = "source_batch_id", columnDefinition = "uuid", nullable = false)
    private UUID sourceBatchId;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt = OffsetDateTime.now(ZoneOffset.UTC);

    public String getEmployeeId() {
        return employeeId;
    }

    public int getVersion() {
        return version;
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

    public LocalDate getEffectiveStart() {
        return effectiveStart;
    }

    public LocalDate getEffectiveEnd() {
        return effectiveEnd;
    }

    public UUID getSourceBatchId() {
        return sourceBatchId;
    }

    public OffsetDateTime getSyncedAt() {
        return syncedAt;
    }

    public boolean sameBusinessState(String fullName,
                                     String email,
                                     String organizationCode,
                                     String employmentType,
                                     String employmentStatus,
                                     LocalDate effectiveStart,
                                     LocalDate effectiveEnd) {
        return safeEquals(this.fullName, fullName)
                && safeEquals(this.email, email)
                && safeEquals(this.organizationCode, organizationCode)
                && safeEquals(this.employmentType, employmentType)
                && safeEquals(this.employmentStatus, employmentStatus)
                && safeEquals(this.effectiveStart, effectiveStart)
                && safeEquals(this.effectiveEnd, effectiveEnd);
    }

    private static boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    public void closeAt(LocalDate endDate) {
        if (endDate != null && effectiveStart != null && endDate.isBefore(effectiveStart)) {
            this.effectiveEnd = effectiveStart;
        } else {
            this.effectiveEnd = endDate;
        }
    }
}
