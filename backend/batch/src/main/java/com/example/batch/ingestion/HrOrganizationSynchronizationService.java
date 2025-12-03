package com.example.batch.ingestion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.dto.HrOrganizationRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;
import com.example.common.cache.CacheNames;

@Service
@RequiredArgsConstructor
public class HrOrganizationSynchronizationService {

    private final HrOrganizationRepository organizationRepository;

    @Transactional
    @CacheEvict(cacheNames = {CacheNames.ORGANIZATION_ROW_SCOPE, CacheNames.DW_ORG_TREE}, allEntries = true)
    public HrSyncResult synchronize(HrImportBatchEntity batch, List<HrOrganizationRecord> records) {
        int inserted = 0;
        int updated = 0;

        for (HrOrganizationRecord record : records) {
            HrOrganizationEntity active = organizationRepository
                    .findFirstByOrganizationCodeAndEffectiveEndIsNullOrderByVersionDesc(record.organizationCode())
                    .orElse(null);
            LocalDate newStart = record.startDate();
            LocalDate newEnd = record.endDate();

            if (active != null && active.sameBusinessState(record.name(), record.parentOrganizationCode(),
                    record.status(), record.leaderEmployeeId(), record.managerEmployeeId(), newStart, newEnd)) {
                continue;
            }

            int nextVersion = active == null ? 1 : active.getVersion() + 1;
            if (active != null) {
                LocalDate closingDate = newStart.minusDays(1);
                if (closingDate.isBefore(active.getEffectiveStart())) {
                    closingDate = active.getEffectiveStart();
                }
                active.closeAt(closingDate);
                organizationRepository.save(active);
                updated++;
            } else {
                inserted++;
            }

            HrOrganizationEntity snapshot = HrOrganizationEntity.snapshot(
                    record.organizationCode(),
                    nextVersion,
                    record.name(),
                    record.parentOrganizationCode(),
                    record.status(),
                    record.leaderEmployeeId(),
                    record.managerEmployeeId(),
                    newStart,
                    newEnd,
                    batch.getId(),
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
            organizationRepository.save(snapshot);
        }
        return new HrSyncResult(inserted, updated);
    }
}
