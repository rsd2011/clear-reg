package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.example.dw.application.port.DwBatchPort;
import com.example.dw.domain.HrBatchStatus;
import com.example.dw.dto.DataFeedType;
import com.example.server.web.dto.DwBatchResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("DwBatchController 테스트")
class DwBatchControllerTest {

    @Mock
    private DwBatchPort batchPort;

    @InjectMocks
    private DwBatchController controller;

    @Nested
    @DisplayName("getBatches")
    class GetBatchesTests {

        @Test
        @DisplayName("Given: 배치 이력 존재 / When: getBatches 호출 / Then: 목록 반환")
        void givenBatches_whenGetBatches_thenReturnList() {
            DwBatchPort.DwBatchRecord record = new DwBatchPort.DwBatchRecord(
                    UUID.randomUUID(), "employee_20250101_001.csv", DataFeedType.EMPLOYEE, "HR_SYSTEM",
                    LocalDate.of(2025, 1, 1), HrBatchStatus.COMPLETED,
                    100, 80, 20, 0, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), null);

            given(batchPort.getBatches(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(record)));

            List<DwBatchResponse> result = controller.getBatches();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).fileName()).isEqualTo("employee_20250101_001.csv");
        }

        @Test
        @DisplayName("Given: 배치 이력 없음 / When: getBatches 호출 / Then: 빈 목록 반환")
        void givenNoBatches_whenGetBatches_thenReturnEmptyList() {
            given(batchPort.getBatches(any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of()));

            List<DwBatchResponse> result = controller.getBatches();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("latest")
    class LatestTests {

        @Test
        @DisplayName("Given: 배치 존재 / When: latest 호출 / Then: 가장 최신 배치 반환")
        void givenBatch_whenLatest_thenReturnBatch() {
            DwBatchPort.DwBatchRecord record = new DwBatchPort.DwBatchRecord(
                    UUID.randomUUID(), "employee_20250102_001.csv", DataFeedType.EMPLOYEE, "HR_SYSTEM",
                    LocalDate.of(2025, 1, 2), HrBatchStatus.COMPLETED,
                    50, 50, 0, 0, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC), null);

            given(batchPort.latestBatch()).willReturn(Optional.of(record));

            ResponseEntity<DwBatchResponse> response = controller.latest();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().fileName()).isEqualTo("employee_20250102_001.csv");
        }

        @Test
        @DisplayName("Given: 배치 없음 / When: latest 호출 / Then: 404 반환")
        void givenNoBatch_whenLatest_thenReturnNotFound() {
            given(batchPort.latestBatch()).willReturn(Optional.empty());

            ResponseEntity<DwBatchResponse> response = controller.latest();

            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
            assertThat(response.getBody()).isNull();
        }
    }
}
