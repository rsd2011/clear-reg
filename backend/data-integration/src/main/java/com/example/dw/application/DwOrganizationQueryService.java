package com.example.dw.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;
import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationTreeService.OrganizationTreeSnapshot;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;

@Service
public class DwOrganizationQueryService {

    private final DwOrganizationTreeService organizationTreeService;
    private final OrganizationRowScopeStrategy customScopeStrategy;
    private final OrganizationReadModelPort readModelPort;
    private final AuditPort auditPort;

    public DwOrganizationQueryService(DwOrganizationTreeService organizationTreeService,
                                      OrganizationRowScopeStrategy customScopeStrategy,
                                      @Nullable OrganizationReadModelPort readModelPort,
                                      AuditPort auditPort) {
        this.organizationTreeService = organizationTreeService;
        this.customScopeStrategy = customScopeStrategy;
        this.readModelPort = readModelPort;
        this.auditPort = auditPort;
    }

    public Page<DwOrganizationNode> getOrganizations(Pageable pageable,
                                                     RowScope rowScope,
                                                     String organizationCode) {
        var rowAccessMatch = com.example.common.policy.RowAccessContextHolder.get();
        if (rowAccessMatch != null && rowAccessMatch.getRowScope() != null) {
            rowScope = rowAccessMatch.getRowScope();
        }

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
        return toPage(candidates, pageable, rowScope, organizationCode);
    }

    private Page<DwOrganizationNode> toPage(List<DwOrganizationNode> nodes, Pageable pageable,
                                           RowScope rowScope, String organizationCode) {
        int start = (int) pageable.getOffset();
        if (start >= nodes.size()) {
            Page<DwOrganizationNode> empty = new PageImpl<>(List.of(), pageable, nodes.size());
            audit(rowScope, organizationCode, nodes.size(), empty.getContent().size(), pageable);
            return empty;
        }
        int end = Math.min(start + pageable.getPageSize(), nodes.size());
        List<DwOrganizationNode> content = nodes.subList(start, end);
        Page<DwOrganizationNode> page = new PageImpl<>(List.copyOf(content), pageable, nodes.size());
        audit(rowScope, organizationCode, nodes.size(), content.size(), pageable);
        return page;
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

    private void audit(RowScope rowScope, String orgCode, long total, long returned, Pageable pageable) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .eventType("DW_ORG_QUERY")
                    .moduleName("data-integration")
                    .action("DW_ORG_LIST")
                    .actor(Actor.builder().id("dw-ingestion").type(ActorType.SYSTEM).build())
                    .subject(Subject.builder().type("ORGANIZATION").key(orgCode != null ? orgCode : "ALL").build())
                    .success(true)
                    .resultCode("OK")
                    .riskLevel(RiskLevel.MEDIUM)
                    .extraEntry("rowScope", rowScope != null ? rowScope.name() : "UNKNOWN")
                    .extraEntry("total", total)
                    .extraEntry("returned", returned)
                    .extraEntry("page", pageable.getPageNumber())
                    .extraEntry("size", pageable.getPageSize())
                    .build();
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (Exception ignore) {
            // 감사 실패가 업무 흐름을 막지 않도록 삼킴
        }
    }
}
