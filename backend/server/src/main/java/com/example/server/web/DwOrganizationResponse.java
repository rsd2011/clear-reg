package com.example.server.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.auth.policy.Sensitive;
import com.example.dwgateway.dw.DwOrganizationPort;

public record DwOrganizationResponse(UUID id,
                                     String organizationCode,
                                     int version,
                                     @Sensitive("ORG_NAME") String name,
                                     String parentOrganizationCode,
                                     String status,
                                     LocalDate effectiveStart,
                                     LocalDate effectiveEnd,
                                     OffsetDateTime syncedAt) {

    public static DwOrganizationResponse fromRecord(DwOrganizationPort.DwOrganizationRecord record) {
        return fromRecord(record, UnaryOperator.identity());
    }

    public static DwOrganizationResponse fromRecord(DwOrganizationPort.DwOrganizationRecord record, UnaryOperator<String> masker) {
        UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;
        return new DwOrganizationResponse(record.id(),
                record.organizationCode(),
                record.version(),
                fn.apply(record.name()),
                record.parentOrganizationCode(),
                record.status(),
                record.effectiveStart(),
                record.effectiveEnd(),
                record.syncedAt());
    }
}
