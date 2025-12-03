package com.example.dw.application.port;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;

import lombok.RequiredArgsConstructor;

/**
 * Adapter implementing DwOrganizationPort using DwOrganizationQueryService.
 */
@Component
@RequiredArgsConstructor
public class DwOrganizationPortAdapter implements DwOrganizationPort {

    private final DwOrganizationQueryService organizationQueryService;

    @Override
    public Page<DwOrganizationRecord> getOrganizations(Pageable pageable, RowScope rowScope, String organizationCode) {
        Page<DwOrganizationNode> nodes = organizationQueryService.getOrganizations(pageable, rowScope, organizationCode);
        return new PageImpl<>(nodes.getContent().stream().map(this::toRecord).toList(),
                pageable, nodes.getTotalElements());
    }

    private DwOrganizationRecord toRecord(DwOrganizationNode node) {
        return new DwOrganizationRecord(node.id(), node.organizationCode(), node.version(),
                node.name(), node.parentOrganizationCode(), node.status(),
                node.effectiveStart(), node.effectiveEnd(), node.syncedAt());
    }
}
