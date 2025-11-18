package com.example.hr.dto;

import java.util.List;

public record HrValidationResult(List<HrEmployeeRecord> validRecords,
                                 List<HrValidationError> errors) {
}
