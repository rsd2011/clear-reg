package com.example.hr.domain;

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
@Table(name = "hr_organizations",
        indexes = {
                @Index(name = "idx_hr_org_code", columnList = "organization_code"),
                @Index(name = "idx_hr_org_active", columnList = "organization_code, effective_end")
        })
public class HrOrganizationEntity extends PrimaryKeyEntity {

    @Column(name = "organization_code", nullable = false, length = 64)
    private String organizationCode;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "parent_code", length = 64)
    private String parentOrganizationCode;

    @Column(name = "status", length = 64)
    private String status;

    @Column(name = "effective_start", nullable = false)
    private LocalDate effectiveStart;

    @Column(name = "effective_end")
    private LocalDate effectiveEnd;

    @Column(name = "source_batch_id", columnDefinition = "uuid", nullable = false)
    private UUID sourceBatchId;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt = OffsetDateTime.now(ZoneOffset.UTC);

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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

    public LocalDate getEffectiveStart() {
        return effectiveStart;
    }

    public void setEffectiveStart(LocalDate effectiveStart) {
        this.effectiveStart = effectiveStart;
    }

    public LocalDate getEffectiveEnd() {
        return effectiveEnd;
    }

    public void setEffectiveEnd(LocalDate effectiveEnd) {
        this.effectiveEnd = effectiveEnd;
    }

    public UUID getSourceBatchId() {
        return sourceBatchId;
    }

    public void setSourceBatchId(UUID sourceBatchId) {
        this.sourceBatchId = sourceBatchId;
    }

    public OffsetDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(OffsetDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }

    public boolean sameBusinessState(String name,
                                     String parentOrganizationCode,
                                     String status,
                                     LocalDate effectiveStart,
                                     LocalDate effectiveEnd) {
        return safeEquals(this.name, name)
                && safeEquals(this.parentOrganizationCode, parentOrganizationCode)
                && safeEquals(this.status, status)
                && safeEquals(this.effectiveStart, effectiveStart)
                && safeEquals(this.effectiveEnd, effectiveEnd);
    }

    private static boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
