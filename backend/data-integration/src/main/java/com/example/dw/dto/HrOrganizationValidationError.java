package com.example.dw.dto;

public record HrOrganizationValidationError(int lineNumber,
                                            String organizationCode,
                                            String errorCode,
                                            String errorMessage,
                                            String rawPayload) {
}
