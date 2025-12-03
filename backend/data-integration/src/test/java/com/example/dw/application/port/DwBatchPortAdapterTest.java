package com.example.dw.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.audit.AuditPort;
import com.example.dw.application.DwBatchQueryService;
import com.example.dw.domain.HrBatchStatus;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;

@ExtendWith(MockitoExtension.class)
@DisplayName("DwBatchPortAdapter 테스트")
class DwBatchPortAdapterTest {

    @Mock
    private DwBatchQueryService batchQueryService;

    @Mock
    private AuditPort auditPort;

    @InjectMocks
    private DwBatchPortAdapter adapter;

    private HrImportBatchEntity createTestEntity() {
        HrImportBatchEntity entity = HrImportBatchEntity.receive(
                "employee_20250101.csv",
                DataFeedType.EMPLOYEE,
                "HR_SYSTEM",
                LocalDate.of(2025, 1, 1),
                1,
                "abc123checksum",
                "/data/hr/employee_20250101.csv");
        entity.markCompleted(80, 15, 5);
        return entity;
    }

    @Test
    @DisplayName("Given 배치가 존재할 때 When 목록 조회하면 Then 배치 레코드를 반환한다")
    void givenBatches_whenListing_thenReturnRecords() {
        doNothing().when(auditPort).record(any(), any());
        HrImportBatchEntity entity = createTestEntity();
        Pageable pageable = PageRequest.of(0, 10);
        given(batchQueryService.getBatches(pageable)).willReturn(new PageImpl<>(List.of(entity)));

        Page<DwBatchPort.DwBatchRecord> result = adapter.getBatches(pageable);

        assertThat(result.getContent()).hasSize(1);
        DwBatchPort.DwBatchRecord record = result.getContent().get(0);
        assertThat(record.fileName()).isEqualTo("employee_20250101.csv");
        assertThat(record.status()).isEqualTo(HrBatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("Given 최신 배치가 존재할 때 When 조회하면 Then 배치를 반환한다")
    void givenLatestBatch_whenQuerying_thenReturnBatch() {
        doNothing().when(auditPort).record(any(), any());
        HrImportBatchEntity entity = createTestEntity();
        given(batchQueryService.latestBatch()).willReturn(Optional.of(entity));

        Optional<DwBatchPort.DwBatchRecord> result = adapter.latestBatch();

        assertThat(result).isPresent();
        assertThat(result.get().feedType()).isEqualTo(DataFeedType.EMPLOYEE);
    }

    @Test
    @DisplayName("Given 배치가 없을 때 When 최신 조회하면 Then 빈 Optional을 반환한다")
    void givenNoBatch_whenQueryingLatest_thenReturnEmpty() {
        doNothing().when(auditPort).record(any(), any());
        given(batchQueryService.latestBatch()).willReturn(Optional.empty());

        Optional<DwBatchPort.DwBatchRecord> result = adapter.latestBatch();

        assertThat(result).isEmpty();
    }
}
