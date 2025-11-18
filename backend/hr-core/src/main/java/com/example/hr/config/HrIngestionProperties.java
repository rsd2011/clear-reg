package com.example.hr.config;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "hr.ingestion")
public class HrIngestionProperties {

    private boolean enabled = true;

    @NotBlank
    private String batchCron = "0 30 2 * * *";

    @NotBlank
    private String timezone = "Asia/Seoul";

    @NotNull
    private Path incomingDir = Path.of("build/hr/incoming");

    @NotNull
    private Path archiveDir = Path.of("build/hr/archive");

    @NotNull
    private Path errorDir = Path.of("build/hr/error");

    @NotNull
    private Duration retention = Duration.ofDays(90);

    @NotNull
    private Duration reconciliationLookback = Duration.ofDays(30);

    private boolean autoArchive = true;

    @NotNull
    private DatabaseProperties database = new DatabaseProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBatchCron() {
        return batchCron;
    }

    public void setBatchCron(String batchCron) {
        this.batchCron = batchCron;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Path getIncomingDir() {
        return incomingDir;
    }

    public void setIncomingDir(Path incomingDir) {
        this.incomingDir = incomingDir;
    }

    public Path getArchiveDir() {
        return archiveDir;
    }

    public void setArchiveDir(Path archiveDir) {
        this.archiveDir = archiveDir;
    }

    public Path getErrorDir() {
        return errorDir;
    }

    public void setErrorDir(Path errorDir) {
        this.errorDir = errorDir;
    }

    public Duration getRetention() {
        return retention;
    }

    public void setRetention(Duration retention) {
        this.retention = retention;
    }

    public Duration getReconciliationLookback() {
        return reconciliationLookback;
    }

    public void setReconciliationLookback(Duration reconciliationLookback) {
        this.reconciliationLookback = reconciliationLookback;
    }

    public boolean isAutoArchive() {
        return autoArchive;
    }

    public void setAutoArchive(boolean autoArchive) {
        this.autoArchive = autoArchive;
    }

    public DatabaseProperties getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseProperties database) {
        this.database = database;
    }

    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }

    public static class DatabaseProperties {

        private boolean enabled = false;
        private int batchSize = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
