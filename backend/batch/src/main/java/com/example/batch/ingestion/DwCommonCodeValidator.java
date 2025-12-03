package com.example.batch.ingestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.dw.dto.DwCommonCodeRecord;

@Component
public class DwCommonCodeValidator {

    public DwCommonCodeValidationResult validate(List<DwCommonCodeRecord> records) {
        List<DwCommonCodeRecord> valid = new ArrayList<>();
        List<DwCommonCodeValidationError> errors = new ArrayList<>();
        Set<String> duplicates = new HashSet<>();

        for (int i = 0; i < records.size(); i++) {
            DwCommonCodeRecord record = records.get(i);
            if (!StringUtils.hasText(record.codeType())) {
                errors.add(new DwCommonCodeValidationError(record.lineNumber(), "코드 유형이 비어 있습니다."));
                continue;
            }
            if (!StringUtils.hasText(record.codeValue())) {
                errors.add(new DwCommonCodeValidationError(record.lineNumber(), "코드 값이 비어 있습니다."));
                continue;
            }
            if (!StringUtils.hasText(record.codeName())) {
                errors.add(new DwCommonCodeValidationError(record.lineNumber(), "코드명이 비어 있습니다."));
                continue;
            }
            String key = record.codeType().toUpperCase(Locale.ROOT) + "|" + record.codeValue().toUpperCase(Locale.ROOT);
            if (!duplicates.add(key)) {
                errors.add(new DwCommonCodeValidationError(record.lineNumber(), "중복된 타입/값 조합입니다."));
                continue;
            }
            valid.add(record);
        }
        return new DwCommonCodeValidationResult(valid, errors);
    }
}
