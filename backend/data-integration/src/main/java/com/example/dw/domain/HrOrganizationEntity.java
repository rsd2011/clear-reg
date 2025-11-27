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
@Table(name = "dw_organizations",
        indexes = {
                @Index(name = "idx_dw_org_code", columnList = "organization_code"),
                @Index(name = "idx_dw_org_active", columnList = "organization_code, effective_end")
        })
public class HrOrganizationEntity extends PrimaryKeyEntity {

    protected HrOrganizationEntity() {
    }

    private HrOrganizationEntity(String organizationCode,
                                 int version,
                                 String name,
                                 String parentOrganizationCode,
                                 String status,
                                 String leaderEmployeeId,
                                 String managerEmployeeId,
                                 LocalDate effectiveStart,
                                 LocalDate effectiveEnd,
                                 UUID sourceBatchId,
                                 OffsetDateTime syncedAt) {
        this.organizationCode = organizationCode;
        this.version = version;
        this.name = name;
        this.parentOrganizationCode = parentOrganizationCode;
        this.status = status;
        this.leaderEmployeeId = leaderEmployeeId;
        this.managerEmployeeId = managerEmployeeId;
        this.effectiveStart = effectiveStart;
        this.effectiveEnd = effectiveEnd;
        this.sourceBatchId = sourceBatchId;
        this.syncedAt = syncedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : syncedAt;
    }

    public static HrOrganizationEntity snapshot(String organizationCode,
                                                int version,
                                                String name,
                                                String parentOrganizationCode,
                                                String status,
                                                String leaderEmployeeId,
                                                String managerEmployeeId,
                                                LocalDate effectiveStart,
                                                LocalDate effectiveEnd,
                                                UUID sourceBatchId,
                                                OffsetDateTime syncedAt) {
        return new HrOrganizationEntity(organizationCode, version, name, parentOrganizationCode, status,
                leaderEmployeeId, managerEmployeeId, effectiveStart, effectiveEnd, sourceBatchId, syncedAt);
    }

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

    /** 해당 조직의 리더 직원번호 */
    @Column(name = "leader_employee_id", length = 64)
    private String leaderEmployeeId;

    /** 해당 조직의 업무 매니저 직원번호 */
    @Column(name = "manager_employee_id", length = 64)
    private String managerEmployeeId;

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

    public int getVersion() {
        return version;
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

    public String getLeaderEmployeeId() {
        return leaderEmployeeId;
    }

    public String getManagerEmployeeId() {
        return managerEmployeeId;
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

    public boolean sameBusinessState(String name,
                                     String parentOrganizationCode,
                                     String status,
                                     String leaderEmployeeId,
                                     String managerEmployeeId,
                                     LocalDate effectiveStart,
                                     LocalDate effectiveEnd) {
        return safeEquals(this.name, name)
                && safeEquals(this.parentOrganizationCode, parentOrganizationCode)
                && safeEquals(this.status, status)
                && safeEquals(this.leaderEmployeeId, leaderEmployeeId)
                && safeEquals(this.managerEmployeeId, managerEmployeeId)
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
