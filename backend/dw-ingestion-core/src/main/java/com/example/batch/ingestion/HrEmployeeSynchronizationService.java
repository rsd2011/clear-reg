package com.example.batch.ingestion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.dw.application.DwEmployeeDirectoryService;
import com.example.dw.domain.HrEmployeeEntity;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.HrEmployeeRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;

@Service
@RequiredArgsConstructor
public class HrEmployeeSynchronizationService {

    private final HrEmployeeRepository employeeRepository;
    private final DwEmployeeDirectoryService employeeDirectoryService;

    @Transactional
    public HrSyncResult synchronize(HrImportBatchEntity batch, List<HrEmployeeRecord> records) {
        int inserted = 0;
        int updated = 0;

        for (HrEmployeeRecord record : records) {
            HrEmployeeEntity active = employeeRepository.findActive(record.employeeId()).orElse(null);
            LocalDate newStart = record.startDate();
            LocalDate newEnd = record.endDate();

            if (active != null && active.sameBusinessState(record.fullName(), record.email(),
                    record.organizationCode(), record.employmentType(), record.employmentStatus(), newStart, newEnd)) {
                continue;
            }

            int nextVersion = active == null ? 1 : active.getVersion() + 1;
            if (active != null) {
                LocalDate closingDate = newStart.minusDays(1);
                if (closingDate.isBefore(active.getEffectiveStart())) {
                    closingDate = active.getEffectiveStart();
                }
                active.setEffectiveEnd(closingDate);
                employeeRepository.save(active);
                updated++;
            } else {
                inserted++;
            }

            HrEmployeeEntity snapshot = new HrEmployeeEntity();
            snapshot.setEmployeeId(record.employeeId());
            snapshot.setVersion(nextVersion);
            snapshot.setFullName(record.fullName());
            snapshot.setEmail(record.email());
            snapshot.setOrganizationCode(record.organizationCode());
            snapshot.setEmploymentType(record.employmentType());
            snapshot.setEmploymentStatus(record.employmentStatus());
            snapshot.setEffectiveStart(newStart);
            snapshot.setEffectiveEnd(newEnd);
            snapshot.setSourceBatchId(batch.getId());
            snapshot.setSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
            employeeRepository.save(snapshot);
            employeeDirectoryService.evict(record.employeeId());
        }
        return new HrSyncResult(inserted, updated);
    }
}
