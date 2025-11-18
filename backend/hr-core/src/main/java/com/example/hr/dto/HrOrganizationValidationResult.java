package com.example.hr.dto;

import java.util.List;

public record HrOrganizationValidationResult(List<HrOrganizationRecord> validRecords,
                                             List<HrOrganizationValidationError> errors) {
}
