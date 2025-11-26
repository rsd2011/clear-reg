package com.example.dw.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "dw_holidays",
        indexes = {
                @Index(name = "idx_dw_holiday_date_country", columnList = "holiday_date, country_code", unique = true)
        })
public class DwHolidayEntity extends PrimaryKeyEntity {

    protected DwHolidayEntity() {
    }

    private DwHolidayEntity(LocalDate holidayDate,
                            String countryCode,
                            String localName,
                            String englishName,
                            boolean workingDay,
                            UUID sourceBatchId,
                            OffsetDateTime syncedAt) {
        this.holidayDate = holidayDate;
        this.countryCode = countryCode;
        this.localName = localName;
        this.englishName = englishName;
        this.workingDay = workingDay;
        this.sourceBatchId = sourceBatchId;
        this.syncedAt = syncedAt;
    }

    public static DwHolidayEntity create(LocalDate date,
                                         String countryCode,
                                         String localName,
                                         String englishName,
                                         boolean workingDay,
                                         UUID sourceBatchId,
                                         OffsetDateTime syncedAt) {
        return new DwHolidayEntity(date, countryCode, localName, englishName, workingDay,
                sourceBatchId, syncedAt);
    }

    public void updateFromRecord(String localName, String englishName, boolean workingDay, UUID sourceBatchId, OffsetDateTime syncedAt) {
        this.localName = localName;
        this.englishName = englishName;
        this.workingDay = workingDay;
        this.sourceBatchId = sourceBatchId;
        this.syncedAt = syncedAt;
    }

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode;

    @Column(name = "local_name", nullable = false, length = 255)
    private String localName;

    @Column(name = "english_name", length = 255)
    private String englishName;

    @Column(name = "working_day", nullable = false)
    private boolean workingDay;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

    @Column(name = "source_batch_id", columnDefinition = "uuid", nullable = false)
    private UUID sourceBatchId;

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getLocalName() {
        return localName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public boolean isWorkingDay() {
        return workingDay;
    }

    public OffsetDateTime getSyncedAt() {
        return syncedAt;
    }

    public UUID getSourceBatchId() {
        return sourceBatchId;
    }

    public boolean sameBusinessState(String localName,
                                     String englishName,
                                     boolean workingDay) {
        return safeEquals(this.localName, localName)
                && safeEquals(this.englishName, englishName)
                && this.workingDay == workingDay;
    }

    private static boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
