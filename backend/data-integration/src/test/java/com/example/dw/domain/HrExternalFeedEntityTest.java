package com.example.dw.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.dw.dto.DataFeedType;

class HrExternalFeedEntityTest {

    @Test
    @DisplayName("외부 피드 엔티티는 상태 전이에 따라 status와 updatedAt을 갱신한다")
    void stateTransitionsUpdateStatusAndTimestamp() {
        HrExternalFeedEntity entity = HrExternalFeedEntity.receive(
                DataFeedType.EMPLOYEE,
                "payload",
                LocalDate.parse("2024-02-01"),
                1,
                "external-db"
        );

        OffsetDateTime before = entity.getUpdatedAt();
        entity.markProcessing();
        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.PROCESSING);
        assertThat(entity.getUpdatedAt()).isAfter(before);

        entity.markFailed("err");
        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.FAILED);
        assertThat(entity.getUpdatedAt()).isAfter(before);

        entity.markCompleted();
        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.COMPLETED);
        assertThat(entity.getErrorMessage()).isNull();
        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    @Test
    @DisplayName("markFailed는 오류 메시지를 보존하고 markCompleted는 이를 초기화한다")
    void markFailedLeavesErrorUntilCompleted() {
        HrExternalFeedEntity entity = HrExternalFeedEntity.receive(
                DataFeedType.EMPLOYEE,
                "payload",
                LocalDate.parse("2024-02-02"),
                2,
                "external-db"
        );

        entity.markFailed("temporary");
        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.FAILED);
        assertThat(entity.getErrorMessage()).isEqualTo("temporary");

        entity.markCompleted();
        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.COMPLETED);
        assertThat(entity.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("receive는 필수 필드를 세팅하고 기본 sourceSystem을 유지한다")
    void receiveSetsFields() {
        HrExternalFeedEntity entity = HrExternalFeedEntity.receive(
                DataFeedType.COMMON_CODE,
                "data",
                LocalDate.parse("2024-03-01"),
                3,
                "custom-src"
        );

        assertThat(entity.getFeedType()).isEqualTo(DataFeedType.COMMON_CODE);
        assertThat(entity.getPayload()).isEqualTo("data");
        assertThat(entity.getBusinessDate()).isEqualTo(LocalDate.parse("2024-03-01"));
        assertThat(entity.getSequenceNumber()).isEqualTo(3);
        assertThat(entity.getSourceSystem()).isEqualTo("custom-src");
        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.PENDING);
    }
}
