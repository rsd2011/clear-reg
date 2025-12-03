package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.domain.DwCommonCodeEntity;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.DwCommonCodeRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.infrastructure.persistence.DwCommonCodeRepository;

@ExtendWith(MockitoExtension.class)
class DwCommonCodeSynchronizationServiceTest {

    @Mock
    private DwCommonCodeRepository repository;

    private DwCommonCodeSynchronizationService service;

    @BeforeEach
    void setUp() {
        service = new DwCommonCodeSynchronizationService(repository);
    }

    @Test
    void givenNewRecord_whenSynchronize_thenInsert() {
        HrImportBatchEntity batch = HrImportBatchEntity.receive(
                "common.csv", DataFeedType.COMMON_CODE, "SRC", LocalDate.now(), 1, "chk", "/tmp"
        );
        DwCommonCodeRecord record = new DwCommonCodeRecord("CATEGORY", "A", "Alpha", 1, true, null, null, null, 2);
        given(repository.findFirstByCodeTypeAndCodeValue("CATEGORY", "A")).willReturn(Optional.empty());

        HrSyncResult result = service.synchronize(batch, List.of(record));

        assertThat(result.insertedRecords()).isEqualTo(1);
        ArgumentCaptor<DwCommonCodeEntity> captor = ArgumentCaptor.forClass(DwCommonCodeEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCodeName()).isEqualTo("Alpha");
    }

    @Test
    void givenExistingButDifferentRecord_whenSynchronize_thenUpdate() {
        HrImportBatchEntity batch = new HrImportBatchEntity();
        DwCommonCodeEntity existing = DwCommonCodeEntity.create(
                "CATEGORY", "A", "Alpha", 1, true, null, null, null, UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC)
        );
        DwCommonCodeRecord record = new DwCommonCodeRecord("CATEGORY", "A", "Alpha+", 1, true, null, null, null, 2);
        given(repository.findFirstByCodeTypeAndCodeValue("CATEGORY", "A")).willReturn(Optional.of(existing));

        HrSyncResult result = service.synchronize(batch, List.of(record));

        assertThat(result.updatedRecords()).isEqualTo(1);
        verify(repository).save(existing);
    }

    @Test
    void givenSameState_whenSynchronize_thenSkip() {
        HrImportBatchEntity batch = new HrImportBatchEntity();
        DwCommonCodeEntity existing = DwCommonCodeEntity.create(
                "CATEGORY", "A", "Alpha", 1, true, null, null, null, UUID.randomUUID(), OffsetDateTime.now(ZoneOffset.UTC)
        );
        DwCommonCodeRecord record = new DwCommonCodeRecord("CATEGORY", "A", "Alpha", 1, true, null, null, null, 2);
        given(repository.findFirstByCodeTypeAndCodeValue("CATEGORY", "A")).willReturn(Optional.of(existing));

        HrSyncResult result = service.synchronize(batch, List.of(record));

        assertThat(result.updatedRecords()).isZero();
    }
}
