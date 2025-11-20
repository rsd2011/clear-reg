package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.domain.DwHolidayEntity;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DwHolidayRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.infrastructure.persistence.DwHolidayRepository;

@ExtendWith(MockitoExtension.class)
class DwHolidaySynchronizationServiceTest {

    @Mock
    private DwHolidayRepository repository;

    @InjectMocks
    private DwHolidaySynchronizationService service;

    @Test
    void givenNewHoliday_whenSynchronize_thenInsert() {
        DwHolidayRecord record = new DwHolidayRecord(LocalDate.of(2024, 5, 5), "KR", "Children's Day", "Children's Day", false);
        when(repository.findFirstByHolidayDateAndCountryCode(record.date(), record.countryCode())).thenReturn(Optional.empty());

        HrSyncResult result = service.synchronize(new HrImportBatchEntity(), List.of(record));

        assertThat(result.insertedRecords()).isEqualTo(1);
        verify(repository).save(any(DwHolidayEntity.class));
    }

    @Test
    void givenExistingHoliday_whenChanged_thenUpdate() {
        DwHolidayEntity entity = new DwHolidayEntity();
        entity.setHolidayDate(LocalDate.of(2024, 5, 5));
        entity.setCountryCode("KR");
        entity.setLocalName("Children's Day");
        entity.setEnglishName("Children's Day");
        entity.setWorkingDay(false);
        when(repository.findFirstByHolidayDateAndCountryCode(entity.getHolidayDate(), entity.getCountryCode()))
                .thenReturn(Optional.of(entity));

        DwHolidayRecord record = new DwHolidayRecord(LocalDate.of(2024, 5, 5), "KR", "어린이날", "Children's Day", false);

        HrSyncResult result = service.synchronize(new HrImportBatchEntity(), List.of(record));

        assertThat(result.updatedRecords()).isEqualTo(1);
        verify(repository).save(entity);
    }
}
