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
        HrExternalFeedEntity entity = new HrExternalFeedEntity();
        entity.setFeedType(DataFeedType.EMPLOYEE);
        entity.setPayload("payload");
        entity.setBusinessDate(LocalDate.parse("2024-02-01"));
        entity.setSequenceNumber(1);

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
        HrExternalFeedEntity entity = new HrExternalFeedEntity();
        entity.setFeedType(DataFeedType.EMPLOYEE);
        entity.setPayload("payload");
        entity.setBusinessDate(LocalDate.parse("2024-02-02"));
        entity.setSequenceNumber(2);

        entity.markFailed("temporary");
        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.FAILED);
        assertThat(entity.getErrorMessage()).isEqualTo("temporary");

        entity.markCompleted();
        assertThat(entity.getStatus()).isEqualTo(HrExternalFeedStatus.COMPLETED);
        assertThat(entity.getErrorMessage()).isNull();
    }
}
