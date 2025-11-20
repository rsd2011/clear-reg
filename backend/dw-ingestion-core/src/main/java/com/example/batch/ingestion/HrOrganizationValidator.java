package com.example.batch.ingestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.dw.dto.HrOrganizationRecord;
import com.example.dw.dto.HrOrganizationValidationError;
import com.example.dw.dto.HrOrganizationValidationResult;

@Component
public class HrOrganizationValidator {

    public HrOrganizationValidationResult validate(List<HrOrganizationRecord> records) {
        List<HrOrganizationRecord> valid = new ArrayList<>();
        List<HrOrganizationValidationError> errors = new ArrayList<>();
        Set<String> uniqueCodes = new HashSet<>();

        for (HrOrganizationRecord record : records) {
            List<String> validationErrors = new ArrayList<>();
            if (!StringUtils.hasText(record.organizationCode())) {
                validationErrors.add("Organization code is required");
            }
            if (!StringUtils.hasText(record.name())) {
                validationErrors.add("Name is required");
            }
            if (record.startDate() == null) {
                validationErrors.add("Start date is required");
            }
            if (record.endDate() != null && record.startDate() != null
                    && record.endDate().isBefore(record.startDate())) {
                validationErrors.add("End date must not be before start date");
            }
            if (record.organizationCode() != null && !uniqueCodes.add(record.organizationCode() + "|" + record.startDate())) {
                validationErrors.add("Duplicate organization snapshot");
            }
            if (validationErrors.isEmpty()) {
                valid.add(record);
            } else {
                errors.add(new HrOrganizationValidationError(record.lineNumber(),
                        record.organizationCode(),
                        "VALIDATION",
                        String.join("; ", validationErrors),
                        record.rawPayload()));
            }
        }

        return new HrOrganizationValidationResult(valid, errors);
    }
}
