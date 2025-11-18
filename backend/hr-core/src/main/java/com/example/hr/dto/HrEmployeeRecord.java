package com.example.hr.dto;

import java.time.LocalDate;

public record HrEmployeeRecord(
        String employeeId,
        String fullName,
        String email,
        String organizationCode,
        String employmentType,
        String employmentStatus,
        LocalDate startDate,
        LocalDate endDate,
        String rawPayload,
        int lineNumber) {

    public boolean isActive() {
        return endDate == null || endDate.isAfter(LocalDate.now());
    }
}
