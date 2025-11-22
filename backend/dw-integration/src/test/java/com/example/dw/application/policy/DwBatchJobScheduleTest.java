package com.example.dw.application.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwBatchJobScheduleTest {

    @Test
    @DisplayName("timezone이 null이면 withDefaults로 대체한다")
    void withDefaultsReplacesTimezone() {
        DwBatchJobSchedule schedule = new DwBatchJobSchedule("JOB", true, "0 0 * * * *", null);

        DwBatchJobSchedule applied = schedule.withDefaults("Asia/Seoul");

        assertThat(applied.timezone()).isEqualTo("Asia/Seoul");
        assertThat(applied.cronExpression()).isEqualTo("0 0 * * * *");
    }

    @Test
    @DisplayName("필수 필드가 null이면 생성 시 예외를 던진다")
    void throwsWhenRequiredFieldsNull() {
        assertThatThrownBy(() -> new DwBatchJobSchedule(null, true, "0 0 * * * *", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DwBatchJobSchedule("JOB", true, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
