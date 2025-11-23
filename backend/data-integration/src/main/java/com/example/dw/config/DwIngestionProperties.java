package com.example.dw.config;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "dw.ingestion")
public class DwIngestionProperties {

    private boolean enabled = true;

    @NotBlank
    private String batchCron = "0 30 2 * * *";

    @NotBlank
    private String timezone = "Asia/Seoul";

    @NotNull
    private Path incomingDir = Path.of("build/dw/incoming");

    @NotNull
    private Path archiveDir = Path.of("build/dw/archive");

    @NotNull
    private Path errorDir = Path.of("build/dw/error");

    @NotNull
    private Duration retention = Duration.ofDays(90);

    @NotNull
    private Duration reconciliationLookback = Duration.ofDays(30);

    private boolean autoArchive = true;

    @NotNull
    private DatabaseProperties database = new DatabaseProperties();

    @NotNull
    @Valid
    private List<JobScheduleProperties> jobSchedules = defaultJobSchedules();

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

    public List<JobScheduleProperties> getJobSchedules() {
        return jobSchedules;
    }

    public void setJobSchedules(List<JobScheduleProperties> jobSchedules) {
        if (jobSchedules == null || jobSchedules.isEmpty()) {
            this.jobSchedules = defaultJobSchedules();
        } else {
            this.jobSchedules = new ArrayList<>(jobSchedules);
        }
    }

    private List<JobScheduleProperties> defaultJobSchedules() {
        JobScheduleProperties schedule = new JobScheduleProperties();
        schedule.setJobKey("DW_INGESTION");
        schedule.setEnabled(true);
        schedule.setCronExpression(this.batchCron);
        schedule.setTimezone(this.timezone);
        List<JobScheduleProperties> defaults = new ArrayList<>();
        defaults.add(schedule);
        return defaults;
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

    public static class JobScheduleProperties {

        @NotBlank
        private String jobKey = "DW_INGESTION";

        private boolean enabled = true;

        @NotBlank
        private String cronExpression = "0 30 2 * * *";

        private String timezone;

        public String getJobKey() {
            return jobKey;
        }

        public void setJobKey(String jobKey) {
            this.jobKey = jobKey;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }
    }
}
