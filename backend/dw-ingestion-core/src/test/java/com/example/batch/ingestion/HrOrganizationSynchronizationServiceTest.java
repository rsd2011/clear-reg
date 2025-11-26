package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.HrOrganizationRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;

@ExtendWith(MockitoExtension.class)
class HrOrganizationSynchronizationServiceTest {

    @Mock
    private HrOrganizationRepository repository;

    private HrOrganizationSynchronizationService service;

    @BeforeEach
    void setUp() {
        service = new HrOrganizationSynchronizationService(repository);
    }

    @Test
    void givenNoActiveRecord_whenSynchronize_thenInsert() {
        HrOrganizationRecord record = new HrOrganizationRecord("ORG", "Org", null, "ACTIVE",
                LocalDate.now(), null, "payload", 1);
        given(repository.findFirstByOrganizationCodeAndEffectiveEndIsNullOrderByVersionDesc("ORG"))
                .willReturn(Optional.empty());

        HrImportBatchEntity batch = HrImportBatchEntity.receive(
                "org.csv", DataFeedType.ORGANIZATION, "SRC", LocalDate.now(), 1, "chk", "/tmp"
        );
        HrSyncResult result = service.synchronize(batch, List.of(record));

        assertThat(result.insertedRecords()).isEqualTo(1);
        verify(repository).save(any(HrOrganizationEntity.class));
    }

    @Test
    void givenActiveRecord_whenStateDiffers_thenUpdateAndInsert() {
        HrOrganizationEntity active = HrOrganizationEntity.snapshot(
                "ORG",
                1,
                "Org",
                null,
                "ACTIVE",
                LocalDate.now().minusDays(5),
                null,
                java.util.UUID.randomUUID(),
                java.time.OffsetDateTime.now()
        );
        HrOrganizationRecord record = new HrOrganizationRecord("ORG", "Org", null, "INACTIVE",
                LocalDate.now(), null, "payload", 2);
        given(repository.findFirstByOrganizationCodeAndEffectiveEndIsNullOrderByVersionDesc("ORG"))
                .willReturn(Optional.of(active));

        HrSyncResult result = service.synchronize(new HrImportBatchEntity(), List.of(record));

        assertThat(result.updatedRecords()).isEqualTo(1);
        verify(repository).save(active);
    }
}
