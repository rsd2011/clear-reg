package com.example.batch.ingestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.dw.dto.DwHolidayRecord;

@Component
public class DwHolidayValidator {

    public DwHolidayValidationResult validate(List<DwHolidayRecord> records) {
        List<DwHolidayRecord> valid = new ArrayList<>();
        List<DwHolidayValidationError> errors = new ArrayList<>();
        Set<String> keyTracker = new HashSet<>();
        for (int i = 0; i < records.size(); i++) {
            DwHolidayRecord record = records.get(i);
            if (record.countryCode() == null || record.countryCode().isBlank()) {
                errors.add(new DwHolidayValidationError(i + 1, "국가 코드가 비어 있습니다."));
                continue;
            }
            if (record.localName() == null || record.localName().isBlank()) {
                errors.add(new DwHolidayValidationError(i + 1, "휴일명이 비어 있습니다."));
                continue;
            }
            String key = record.date() + "_" + record.countryCode().toUpperCase(Locale.ROOT);
            if (!keyTracker.add(key)) {
                errors.add(new DwHolidayValidationError(i + 1, "중복된 날짜/국가 조합입니다."));
                continue;
            }
            valid.add(record);
        }
        return new DwHolidayValidationResult(valid, errors);
    }
}
