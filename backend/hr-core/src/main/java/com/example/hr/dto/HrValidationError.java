package com.example.hr.dto;

public record HrValidationError(int lineNumber,
                                String employeeId,
                                String errorCode,
                                String errorMessage,
                                String rawPayload) {
}
