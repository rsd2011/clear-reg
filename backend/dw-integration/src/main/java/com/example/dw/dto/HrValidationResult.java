package com.example.dw.dto;

import java.util.List;

public record HrValidationResult(List<HrEmployeeRecord> validRecords,
                                 List<HrValidationError> errors) {
}
