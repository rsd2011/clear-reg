package com.example.dw.dto;

import java.time.LocalDate;

public record HrOrganizationRecord(String organizationCode,
                                   String name,
                                   String parentOrganizationCode,
                                   String status,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   String rawPayload,
                                   int lineNumber) {
}
