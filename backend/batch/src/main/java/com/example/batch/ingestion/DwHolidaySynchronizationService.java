package com.example.batch.ingestion;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.dw.domain.DwHolidayEntity;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DwHolidayRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.infrastructure.persistence.DwHolidayRepository;

@Service
@RequiredArgsConstructor
public class DwHolidaySynchronizationService {

    private final DwHolidayRepository holidayRepository;

    @Transactional
    public HrSyncResult synchronize(HrImportBatchEntity batch, List<DwHolidayRecord> records) {
        int inserted = 0;
        int updated = 0;

        for (DwHolidayRecord record : records) {
            DwHolidayEntity existing = holidayRepository.findFirstByHolidayDateAndCountryCode(record.date(),
                    record.countryCode()).orElse(null);
            if (existing != null && existing.sameBusinessState(record.localName(), record.englishName(), record.workingDay())) {
                continue;
            }
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if (existing == null) {
                inserted++;
                existing = DwHolidayEntity.create(
                        record.date(),
                        record.countryCode(),
                        record.localName(),
                        record.englishName(),
                        record.workingDay(),
                        batch.getId(),
                        now
                );
            } else {
                updated++;
                existing.updateFromRecord(record.localName(), record.englishName(), record.workingDay(), batch.getId(), now);
            }
            holidayRepository.save(existing);
        }
        return new HrSyncResult(inserted, updated);
    }
}
