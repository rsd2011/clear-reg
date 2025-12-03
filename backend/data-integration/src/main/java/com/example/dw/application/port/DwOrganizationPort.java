package com.example.dw.application.port;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.common.security.RowScope;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Port interface for organization queries.
 */
public interface DwOrganizationPort {

    Page<DwOrganizationRecord> getOrganizations(Pageable pageable, RowScope rowScope, String organizationCode);

    record DwOrganizationRecord(UUID id,
                                String organizationCode,
                                int version,
                                String name,
                                String parentOrganizationCode,
                                String status,
                                LocalDate effectiveStart,
                                LocalDate effectiveEnd,
                                OffsetDateTime syncedAt) { }
}
