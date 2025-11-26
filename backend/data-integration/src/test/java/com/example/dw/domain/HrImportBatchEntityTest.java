package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DataFeedType;

class HrImportBatchEntityTest {

    @Test
    @DisplayName("검증 완료 처리 시 상태와 레코드 수가 갱신된다")
    void markValidated_updatesStatusAndCounts() {
        HrImportBatchEntity entity = HrImportBatchEntity.receive(
                "file.csv",
                DataFeedType.EMPLOYEE,
                "SRC",
                LocalDate.parse("2024-02-01"),
                1,
                "chk",
                "/tmp/file.csv"
        );

        entity.markValidated(10, 1);
        assertThat(entity.getStatus()).isEqualTo(HrBatchStatus.VALIDATED);
        assertThat(entity.getTotalRecords()).isEqualTo(10);
        assertThat(entity.getFailedRecords()).isEqualTo(1);
    }

    @Test
    @DisplayName("완료 처리 시 완료 시각과 반영된 레코드 수가 기록된다")
    void markCompleted_setsCompletionTimeAndCounts() {
        HrImportBatchEntity entity = HrImportBatchEntity.receive(
                "file.csv",
                DataFeedType.EMPLOYEE,
                "SRC",
                LocalDate.parse("2024-02-01"),
                1,
                "chk",
                "/tmp/file.csv"
        );

        entity.markCompleted(5, 4, 1);

        assertThat(entity.getStatus()).isEqualTo(HrBatchStatus.COMPLETED);
        assertThat(entity.getInsertedRecords()).isEqualTo(5);
        assertThat(entity.getUpdatedRecords()).isEqualTo(4);
        assertThat(entity.getFailedRecords()).isEqualTo(1);
        assertThat(entity.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("실패 처리 시 오류 메시지와 완료 시각이 기록된다")
    void markFailed_setsErrorAndCompletionTime() {
        HrImportBatchEntity entity = HrImportBatchEntity.receive(
                "file.csv",
                DataFeedType.EMPLOYEE,
                "SRC",
                LocalDate.parse("2024-02-01"),
                1,
                "chk",
                "/tmp/file.csv"
        );

        entity.markFailed("error");

        assertThat(entity.getStatus()).isEqualTo(HrBatchStatus.FAILED);
        assertThat(entity.getErrorMessage()).isEqualTo("error");
        assertThat(entity.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("저장 시 receivedAt이 비어 있으면 현재 UTC 시간으로 채워진다")
    void prePersist_populatesReceivedAtWhenNull() {
        HrImportBatchEntity entity = HrImportBatchEntity.receive(
                "file.csv",
                DataFeedType.EMPLOYEE,
                "SRC",
                LocalDate.parse("2024-02-01"),
                1,
                "chk",
                "/tmp/file.csv"
        );

        entity.setReceivedAtIfNeeded();

        assertThat(entity.getReceivedAt()).isNotNull();
    }

    @Test
    @DisplayName("receivedAt이 이미 있으면 prePersist가 값을 유지한다")
    void prePersist_keepsExistingReceivedAt() {
        HrImportBatchEntity entity = HrImportBatchEntity.receive(
                "file.csv",
                DataFeedType.EMPLOYEE,
                "SRC",
                LocalDate.parse("2024-02-01"),
                1,
                "chk",
                "/tmp/file.csv"
        );
        var fixed = java.time.OffsetDateTime.parse("2024-01-01T00:00:00Z");
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "receivedAt", fixed);

        entity.setReceivedAtIfNeeded();

        assertThat(entity.getReceivedAt()).isEqualTo(fixed);
    }
}
