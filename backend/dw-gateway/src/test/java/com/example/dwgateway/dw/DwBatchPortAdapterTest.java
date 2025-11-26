package com.example.dwgateway.dw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.audit.AuditPort;
import com.example.dw.application.DwBatchQueryService;
import com.example.dw.domain.HrBatchStatus;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;

@DisplayName("DwBatchPortAdapter 테스트")
class DwBatchPortAdapterTest {

    private final DwBatchQueryService batchQueryService = Mockito.mock(DwBatchQueryService.class);
    private final AuditPort auditPort = Mockito.mock(AuditPort.class);
    private final DwBatchPortAdapter adapter = new DwBatchPortAdapter(batchQueryService, auditPort);

    @Test
    @DisplayName("배치 목록을 레코드로 변환한다")
    void getBatches() {
        Pageable pageable = Pageable.unpaged();
        HrImportBatchEntity entity = sampleEntity();
        given(batchQueryService.getBatches(pageable)).willReturn(new PageImpl<>(java.util.List.of(entity)));

        var page = adapter.getBatches(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).feedType()).isEqualTo(DataFeedType.EMPLOYEE);
        then(batchQueryService).should().getBatches(pageable);
        then(auditPort).should().record(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("최신 배치를 Optional로 반환한다")
    void latestBatch() {
        HrImportBatchEntity entity = sampleEntity();
        given(batchQueryService.latestBatch()).willReturn(Optional.of(entity));

        Optional<DwBatchPort.DwBatchRecord> result = adapter.latestBatch();

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(HrBatchStatus.COMPLETED);
        then(batchQueryService).should().latestBatch();
        then(auditPort).should().record(Mockito.any(), Mockito.any());
    }

    private HrImportBatchEntity sampleEntity() {
        HrImportBatchEntity entity = HrImportBatchEntity.receive(
                "employee_20240101_001.csv",
                DataFeedType.EMPLOYEE,
                "HR",
                LocalDate.of(2024, 1, 1),
                1,
                "chk",
                "/tmp/employee_20240101_001.csv"
        );
        entity.markValidated(10, 0);
        entity.markCompleted(6, 4, 0);
        return entity;
    }
}
