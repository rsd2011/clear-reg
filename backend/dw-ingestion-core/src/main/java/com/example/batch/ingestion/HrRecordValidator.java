package com.example.batch.ingestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.dw.dto.HrEmployeeRecord;
import com.example.dw.dto.HrValidationError;
import com.example.dw.dto.HrValidationResult;

@Component
public class HrRecordValidator {

    public HrValidationResult validate(List<HrEmployeeRecord> records) {
        List<HrEmployeeRecord> valid = new ArrayList<>();
        List<HrValidationError> errors = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();

        for (HrEmployeeRecord record : records) {
            List<String> recordErrors = new ArrayList<>();
            if (!StringUtils.hasText(record.employeeId())) {
                recordErrors.add("Employee ID is required");
            }
            if (!StringUtils.hasText(record.fullName())) {
                recordErrors.add("Full name is required");
            }
            if (record.startDate() == null) {
                recordErrors.add("Start date is required");
            }
            if (record.endDate() != null && record.startDate() != null
                    && record.endDate().isBefore(record.startDate())) {
                recordErrors.add("End date must be after start date");
            }
            String dedupKey = record.employeeId() + "|" + record.startDate();
            if (!uniqueKeys.add(dedupKey)) {
                recordErrors.add("Duplicate employee record for day");
            }

            if (recordErrors.isEmpty()) {
                valid.add(record);
            } else {
                errors.add(new HrValidationError(record.lineNumber(), record.employeeId(), "VALIDATION",
                        String.join("; ", recordErrors), record.rawPayload()));
            }
        }
        return new HrValidationResult(valid, errors);
    }
}
