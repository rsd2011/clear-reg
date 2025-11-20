package com.example.server.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationNode;
import com.example.dw.application.DwOrganizationQueryService;
import com.example.dwgateway.dw.DwOrganizationPort;

@Component
public class DwOrganizationPortAdapter implements DwOrganizationPort {

    private final DwOrganizationQueryService queryService;

    public DwOrganizationPortAdapter(DwOrganizationQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public Page<DwOrganizationRecord> getOrganizations(Pageable pageable,
                                                       RowScope rowScope,
                                                       String organizationCode) {
        Page<DwOrganizationNode> nodes = queryService.getOrganizations(pageable, rowScope, organizationCode);
        return new PageImpl<>(nodes.getContent().stream().map(this::toRecord).toList(),
                pageable, nodes.getTotalElements());
    }

    private DwOrganizationRecord toRecord(DwOrganizationNode node) {
        return new DwOrganizationRecord(node.id(),
                node.organizationCode(),
                node.version(),
                node.name(),
                node.parentOrganizationCode(),
                node.status(),
                node.effectiveStart(),
                node.effectiveEnd(),
                node.syncedAt());
    }
}
