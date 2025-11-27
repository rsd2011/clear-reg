package com.example.dwgateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.dw.domain.HrBatchStatus;
import com.example.dw.dto.DataFeedType;
import com.example.dwgateway.dw.DwBatchPort;
import com.example.dwgateway.web.dto.DwBatchResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("DwBatchController 테스트")
class DwBatchControllerTest {

    @Mock
    private DwBatchPort batchPort;

    @InjectMocks
    private DwBatchController controller;

    private DwBatchPort.DwBatchRecord record;

    @BeforeEach
    void setUp() {
        record = new DwBatchPort.DwBatchRecord(
                java.util.UUID.randomUUID(),
                "employee_20240101_001.csv",
                DataFeedType.EMPLOYEE,
                "HR",
                LocalDate.of(2024, 1, 1),
                HrBatchStatus.COMPLETED,
                10,
                6,
                4,
                0,
                OffsetDateTime.of(2024, 1, 1, 3, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2024, 1, 1, 3, 5, 0, 0, ZoneOffset.UTC),
                null
        );
    }

    @Test
    @DisplayName("Given 배치 목록 When 조회하면 Then DTO 리스트를 반환한다")
    void listBatches() {
        Pageable pageable = Pageable.unpaged();
        given(batchPort.getBatches(pageable)).willReturn(new PageImpl<>(java.util.List.of(record)));

        var response = controller.getBatches();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).fileName()).isEqualTo("employee_20240101_001.csv");
        verify(batchPort).getBatches(pageable);
    }

    @Test
    @DisplayName("Given 최신 배치 When latest 호출 Then ResponseEntity로 반환한다")
    void latestBatch() {
        given(batchPort.latestBatch()).willReturn(Optional.of(record));

        ResponseEntity<DwBatchResponse> response = controller.latest();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HrBatchStatus.COMPLETED);
        verify(batchPort).latestBatch();
    }
}
