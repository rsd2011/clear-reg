package com.example.batch.ingestion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.domain.HrOrganizationEntity;
import com.example.hr.dto.HrOrganizationRecord;
import com.example.hr.dto.HrSyncResult;
import com.example.hr.infrastructure.persistence.HrOrganizationRepository;

@Service
@RequiredArgsConstructor
public class HrOrganizationSynchronizationService {

    private final HrOrganizationRepository organizationRepository;

    @Transactional
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
                    record.status(), newStart, newEnd)) {
                continue;
            }

            int nextVersion = active == null ? 1 : active.getVersion() + 1;
            if (active != null) {
                LocalDate closingDate = newStart.minusDays(1);
                if (closingDate.isBefore(active.getEffectiveStart())) {
                    closingDate = active.getEffectiveStart();
                }
                active.setEffectiveEnd(closingDate);
                organizationRepository.save(active);
                updated++;
            } else {
                inserted++;
            }

            HrOrganizationEntity snapshot = new HrOrganizationEntity();
            snapshot.setOrganizationCode(record.organizationCode());
            snapshot.setVersion(nextVersion);
            snapshot.setName(record.name());
            snapshot.setParentOrganizationCode(record.parentOrganizationCode());
            snapshot.setStatus(record.status());
            snapshot.setEffectiveStart(newStart);
            snapshot.setEffectiveEnd(newEnd);
            snapshot.setSourceBatchId(batch.getId());
            snapshot.setSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
            organizationRepository.save(snapshot);
        }
        return new HrSyncResult(inserted, updated);
    }
}
