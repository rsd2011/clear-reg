package com.example.common.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.AuditPartitionSettings;
import com.example.common.policy.PolicyChangedEvent;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

class SchedulePolicyCoverageTest {

    @Test
    @DisplayName("TriggerDescriptor/BatchJobSchedule validation 및 toTrigger 커버")
    void triggerAndSchedule() {
        TriggerDescriptor cron = new TriggerDescriptor(true, TriggerType.CRON, "0 0 * * * *", 0, 0, ZoneId.of("UTC"));
        assertThat(cron.toTrigger().nextExecution(new org.springframework.scheduling.support.SimpleTriggerContext())).isNotNull();

        TriggerDescriptor delay = new TriggerDescriptor(true, TriggerType.FIXED_DELAY, null, 1000, 10, null);
        assertThat(delay.toTrigger()).isNotNull();

        assertThrows(IllegalArgumentException.class, () -> new TriggerDescriptor(true, TriggerType.CRON, "", 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new TriggerDescriptor(true, TriggerType.FIXED_DELAY, null, 0, 0, null));

        BatchJobSchedule scheduleCron = new BatchJobSchedule(true, TriggerType.CRON, "0 15 * * * *", 0, 0, "Asia/Seoul");
        TriggerDescriptor desc = scheduleCron.toTriggerDescriptor();
        assertThat(desc.expression()).isEqualTo("0 15 * * * *");

        BatchJobSchedule scheduleDelay = new BatchJobSchedule(false, TriggerType.FIXED_DELAY, null, 5000, 100, "");
        assertThat(scheduleDelay.toTriggerDescriptor().fixedDelayMillis()).isEqualTo(5000);
        TriggerDescriptor disabled = new TriggerDescriptor(false, TriggerType.FIXED_DELAY, null, 1000, 0, null);
        assertThat(disabled.toTrigger().nextExecution(new org.springframework.scheduling.support.SimpleTriggerContext())).isNull();

        assertThrows(IllegalArgumentException.class, () -> new BatchJobSchedule(true, TriggerType.CRON, null, 0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new BatchJobSchedule(true, TriggerType.FIXED_DELAY, null, -1, 0, null));
    }

    @Test
    @DisplayName("Policy settings helpers 커버")
    void policyHelpers() {
        AuditPartitionSettings settings = new AuditPartitionSettings(true, "0 0 1 * * *", 1, "hot", "cold", 6, 60);
        assertThat(settings.enabled()).isTrue();
        assertThat(settings.cron()).isEqualTo("0 0 1 * * *");
        assertThat(settings.preloadMonths()).isEqualTo(1);
        assertThat(settings.tablespaceCold()).isEqualTo("cold");
        assertThat(settings.tablespaceHot()).isEqualTo("hot");
        assertThat(settings.hotMonths()).isEqualTo(6);
        assertThat(settings.coldMonths()).isEqualTo(60);
        assertThat(settings.hashCode()).isNotZero();
        assertThat(settings.toString()).contains("AuditPartitionSettings");

        PolicyChangedEvent evt = new PolicyChangedEvent("code", "yaml-body");
        assertThat(evt.payload()).contains("yaml-body");

        PolicySettingsProvider provider = new PolicySettingsProvider() {
            @Override
            public PolicyToggleSettings currentSettings() { return null; }
            @Override
            public BatchJobSchedule batchJobSchedule(BatchJobCode code) { return null; }
        };
        assertThat(provider.currentSettings()).isNull();
        assertThat(provider.batchJobSchedule(BatchJobCode.AUDIT_ARCHIVE)).isNull();
        assertThat(provider.partitionSettings()).isNull();

        PolicySettingsProvider defaults = new PolicySettingsProvider() {
            @Override
            public PolicyToggleSettings currentSettings() { return null; }
        };
        assertThat(defaults.batchJobSchedule(BatchJobCode.AUDIT_ARCHIVE)).isNull();
        assertThat(defaults.partitionSettings()).isNull();
    }
}
