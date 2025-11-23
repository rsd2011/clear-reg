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

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public boolean isWorkingDay() {
        return workingDay;
    }

    public void setWorkingDay(boolean workingDay) {
        this.workingDay = workingDay;
    }

    public OffsetDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(OffsetDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }

    public UUID getSourceBatchId() {
        return sourceBatchId;
    }

    public void setSourceBatchId(UUID sourceBatchId) {
        this.sourceBatchId = sourceBatchId;
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
