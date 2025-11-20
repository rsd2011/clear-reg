package com.example.dw.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationTreeService.OrganizationTreeSnapshot;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;

@Service
public class DwOrganizationQueryService {

    private final DwOrganizationTreeService organizationTreeService;
    private final OrganizationRowScopeStrategy customScopeStrategy;
    private final OrganizationReadModelPort readModelPort;

    public DwOrganizationQueryService(DwOrganizationTreeService organizationTreeService,
                                      OrganizationRowScopeStrategy customScopeStrategy,
                                      @Nullable OrganizationReadModelPort readModelPort) {
        this.organizationTreeService = organizationTreeService;
        this.customScopeStrategy = customScopeStrategy;
        this.readModelPort = readModelPort;
    }

    public Page<DwOrganizationNode> getOrganizations(Pageable pageable,
                                                     RowScope rowScope,
                                                     String organizationCode) {
        if (rowScope == null) {
            throw new IllegalArgumentException("Row scope must be provided");
        }
        if (organizationCode == null && rowScope != RowScope.ALL) {
            throw new IllegalArgumentException("Organization code is required for row scope " + rowScope);
        }
        if (rowScope == RowScope.CUSTOM) {
            return customScopeStrategy.apply(pageable, organizationCode);
        }
        OrganizationTreeSnapshot snapshot = loadSnapshot();
        List<DwOrganizationNode> candidates = switch (rowScope) {
            case OWN -> snapshot.node(organizationCode)
                    .map(List::of)
                    .orElse(List.of());
            case ORG -> snapshot.descendantsIncluding(organizationCode);
            case ALL -> snapshot.flatten();
            default -> List.of();
        };
        return toPage(candidates, pageable);
    }

    private Page<DwOrganizationNode> toPage(List<DwOrganizationNode> nodes, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= nodes.size()) {
            return new PageImpl<>(List.of(), pageable, nodes.size());
        }
        int end = Math.min(start + pageable.getPageSize(), nodes.size());
        List<DwOrganizationNode> content = nodes.subList(start, end);
        return new PageImpl<>(List.copyOf(content), pageable, nodes.size());
    }

    private OrganizationTreeSnapshot loadSnapshot() {
        if (readModelPort != null && readModelPort.isEnabled()) {
            return readModelPort.load()
                    .map(OrganizationTreeReadModel::nodes)
                    .map(DwOrganizationTreeService.OrganizationTreeSnapshot::fromNodes)
                    .orElseGet(organizationTreeService::snapshot);
        }
        return organizationTreeService.snapshot();
    }
}
