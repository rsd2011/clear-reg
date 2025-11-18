package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.hr.domain.HrBatchStatus;
import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.application.HrBatchQueryService;

@ExtendWith(MockitoExtension.class)
class HrBatchControllerTest {

    @Mock
    private HrBatchQueryService batchQueryService;

    @InjectMocks
    private HrBatchController controller;

    private HrImportBatchEntity batch;

    @BeforeEach
    void setUp() {
        batch = createBatch();
    }

    @Test
    void givenAvailableBatches_whenListing_thenReturnPagedResponse() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<HrImportBatchEntity> page = new PageImpl<>(java.util.List.of(batch), pageable, 1);
        given(batchQueryService.getBatches(pageable)).willReturn(page);

        Page<HrBatchResponse> response = controller.getBatches(0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).fileName()).isEqualTo("employee_20240101_001.csv");
        verify(batchQueryService).getBatches(pageable);
    }

    @Test
    void givenLatestBatch_whenRequestingLatest_thenReturnResponseEntity() {
        given(batchQueryService.latestBatch()).willReturn(Optional.of(batch));

        ResponseEntity<HrBatchResponse> response = controller.latest();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HrBatchStatus.COMPLETED);
        verify(batchQueryService).latestBatch();
    }

    private HrImportBatchEntity createBatch() {
        HrImportBatchEntity entity = new HrImportBatchEntity();
        entity.setFileName("employee_20240101_001.csv");
        entity.setBusinessDate(LocalDate.of(2024, 1, 1));
        entity.setSequenceNumber(1);
        entity.setChecksum("checksum");
        entity.setSourcePath("/tmp/hr/employee_20240101_001.csv");
        entity.setStatus(HrBatchStatus.COMPLETED);
        entity.setTotalRecords(10);
        entity.setInsertedRecords(6);
        entity.setUpdatedRecords(4);
        entity.setFailedRecords(0);
        entity.setReceivedAt(OffsetDateTime.of(2024, 1, 1, 3, 0, 0, 0, ZoneOffset.UTC));
        entity.setCompletedAt(OffsetDateTime.of(2024, 1, 1, 3, 5, 0, 0, ZoneOffset.UTC));
        return entity;
    }
}
