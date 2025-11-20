package com.example.dwgateway.client.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationRecordResponse(UUID id,
                                         String organizationCode,
                                         int version,
                                         String name,
                                         String parentOrganizationCode,
                                         String status,
                                         LocalDate effectiveStart,
                                         LocalDate effectiveEnd,
                                         OffsetDateTime syncedAt) {
}
