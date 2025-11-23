package com.example.dw.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwBatchJobScheduleDefaultsTest {

    @Test
    @DisplayName("withDefaults는 timezone이 null이면 fallback을 채운다")
    void fillsFallbackTimezone() {
        DwBatchJobSchedule schedule = new DwBatchJobSchedule("JOB1", true, "0 0 * * *", null);
        DwBatchJobSchedule applied = schedule.withDefaults("Asia/Seoul");
        assertThat(applied.timezone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("withDefaults는 기존 timezone을 유지한다")
    void keepsExistingTimezone() {
        DwBatchJobSchedule schedule = new DwBatchJobSchedule("JOB1", true, "0 0 * * *", "UTC");
        DwBatchJobSchedule applied = schedule.withDefaults("Asia/Seoul");
        assertThat(applied.timezone()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("jobKey나 cron이 null이면 예외를 던진다")
    void constructorValidation() {
        assertThrows(NullPointerException.class, () -> new DwBatchJobSchedule(null, true, "0 0 * * *", "UTC"));
        assertThrows(NullPointerException.class, () -> new DwBatchJobSchedule("JOB1", true, null, "UTC"));
    }
}
