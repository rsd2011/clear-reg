package com.example.batch.ingestion;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.dw.domain.DwCommonCodeEntity;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DwCommonCodeRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.infrastructure.persistence.DwCommonCodeRepository;

@Service
@RequiredArgsConstructor
public class DwCommonCodeSynchronizationService {

    private final DwCommonCodeRepository repository;

    @Transactional
    public HrSyncResult synchronize(HrImportBatchEntity batch, List<DwCommonCodeRecord> records) {
        int inserted = 0;
        int updated = 0;

        for (DwCommonCodeRecord record : records) {
            String normalizedType = record.codeType().toUpperCase(java.util.Locale.ROOT);
            DwCommonCodeEntity entity = repository
                    .findFirstByCodeTypeAndCodeValue(normalizedType, record.codeValue())
                    .orElse(null);
            boolean exists = entity != null;
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if (exists && entity.sameBusinessState(record.codeName(),
                    record.displayOrder() == null ? 0 : record.displayOrder(),
                    record.active(),
                    record.category(),
                    record.description(),
                    record.metadataJson())) {
                continue;
            }
            if (exists) {
                entity.updateFromRecord(record.codeName(),
                        record.displayOrder(),
                        record.active(),
                        record.category(),
                        record.description(),
                        record.metadataJson(),
                        batch.getId(),
                        now);
                updated++;
            } else {
                entity = DwCommonCodeEntity.create(
                        normalizedType,
                        record.codeValue(),
                        record.codeName(),
                        record.displayOrder(),
                        record.active(),
                        record.category(),
                        record.description(),
                        record.metadataJson(),
                        batch.getId(),
                        now
                );
                inserted++;
            }
            repository.save(entity);
        }
        return new HrSyncResult(inserted, updated);
    }
}
