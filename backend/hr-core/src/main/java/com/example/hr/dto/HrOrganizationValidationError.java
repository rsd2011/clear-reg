package com.example.hr.dto;

public record HrOrganizationValidationError(int lineNumber,
                                            String organizationCode,
                                            String errorCode,
                                            String errorMessage,
                                            String rawPayload) {
}
