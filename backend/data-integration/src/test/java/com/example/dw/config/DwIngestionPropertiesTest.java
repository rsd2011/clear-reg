package com.example.dw.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DwIngestionPropertiesTest {

    @Test
    @DisplayName("jobSchedules가 null이면 기본 스케줄로 대체된다")
    void setJobSchedules_replacesNullWithDefaults() {
        DwIngestionProperties properties = new DwIngestionProperties();
        properties.setJobSchedules(null);

        assertThat(properties.getJobSchedules())
                .hasSize(1)
                .allSatisfy(schedule -> {
                    assertThat(schedule.getJobKey()).isEqualTo("DW_INGESTION");
                    assertThat(schedule.isEnabled()).isTrue();
                });
    }

    @Test
    @DisplayName("jobSchedules가 비어 있으면 기본 스케줄로 대체된다")
    void setJobSchedules_replacesEmptyListWithDefaults() {
        DwIngestionProperties properties = new DwIngestionProperties();
        properties.setJobSchedules(List.of());

        assertThat(properties.getJobSchedules())
                .hasSize(1)
                .first()
                .satisfies(schedule -> assertThat(schedule.getCronExpression()).isEqualTo(properties.getBatchCron()));
    }

    @Test
    @DisplayName("jobSchedules를 제공하면 복사본을 유지하며 원본 리스트 변경에 영향받지 않는다")
    void setJobSchedules_keepsCopy() {
        DwIngestionProperties properties = new DwIngestionProperties();
        DwIngestionProperties.JobScheduleProperties custom = new DwIngestionProperties.JobScheduleProperties();
        custom.setJobKey("CUSTOM");
        custom.setCronExpression("0 0 12 * * *");
        custom.setEnabled(false);
        List<DwIngestionProperties.JobScheduleProperties> source = new java.util.ArrayList<>();
        source.add(custom);

        properties.setJobSchedules(source);
        source.clear();

        assertThat(properties.getJobSchedules())
                .singleElement()
                .satisfies(schedule -> {
                    assertThat(schedule.getJobKey()).isEqualTo("CUSTOM");
                    assertThat(schedule.isEnabled()).isFalse();
                });
    }
}
