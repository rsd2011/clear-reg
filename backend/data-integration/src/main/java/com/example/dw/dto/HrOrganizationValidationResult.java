package com.example.dw.dto;

import java.util.List;

public record HrOrganizationValidationResult(List<HrOrganizationRecord> validRecords,
                                             List<HrOrganizationValidationError> errors) {
}
