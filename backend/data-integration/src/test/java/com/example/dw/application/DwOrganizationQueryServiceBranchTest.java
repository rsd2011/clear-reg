package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.example.audit.AuditPort;
import com.example.common.policy.DataPolicyContextHolder;
import com.example.common.policy.DataPolicyMatch;
import com.example.common.security.RowScope;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;

class DwOrganizationQueryServiceBranchTest {

    DwOrganizationTreeService tree = Mockito.mock(DwOrganizationTreeService.class);
    OrganizationRowScopeStrategy custom = Mockito.mock(OrganizationRowScopeStrategy.class);
    OrganizationReadModelPort readModel = Mockito.mock(OrganizationReadModelPort.class);
    AuditPort auditPort = Mockito.mock(AuditPort.class);

    @Test
    @DisplayName("CUSTOM 스코프는 커스텀 전략으로 위임한다")
    void customScopeUsesStrategy() {
        PageRequest page = PageRequest.of(0, 10);
        when(custom.apply(page, "ORG1")).thenReturn(Page.empty(page));
        DwOrganizationQueryService svc = new DwOrganizationQueryService(tree, custom, readModel, auditPort);

        Page<DwOrganizationNode> result = svc.getOrganizations(page, RowScope.CUSTOM, "ORG1");
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("DataPolicyContextHolder rowScope가 있으면 우선 사용한다")
    void policyOverridesRowScope() {
        DataPolicyContextHolder.set(DataPolicyMatch.builder().rowScope(RowScope.ALL).build());
        when(tree.snapshot()).thenReturn(DwOrganizationTreeService.OrganizationTreeSnapshot.fromNodes(List.of()));
        DwOrganizationQueryService svc = new DwOrganizationQueryService(tree, custom, null, auditPort);

        Page<DwOrganizationNode> result = svc.getOrganizations(PageRequest.of(0, 5), RowScope.OWN, "ORG1");
        assertThat(result.getContent()).isEmpty();
        DataPolicyContextHolder.clear();
    }

    @Test
    @DisplayName("organizationCode가 없고 ALL 이외의 스코프이면 예외를 던진다")
    void missingOrgCodeThrows() {
        DwOrganizationQueryService svc = new DwOrganizationQueryService(tree, custom, null, auditPort);
        assertThatThrownBy(() -> svc.getOrganizations(PageRequest.of(0, 1), RowScope.ORG, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("readModelPort가 활성화되면 readmodel을 사용한다")
    void readModelPreferredWhenEnabled() {
        when(readModel.isEnabled()).thenReturn(true);
        OrganizationTreeReadModel rm = new OrganizationTreeReadModel("id", OffsetDateTime.now(), List.of());
        when(readModel.load()).thenReturn(Optional.of(rm));
        DwOrganizationQueryService svc = new DwOrganizationQueryService(tree, custom, readModel, auditPort);

        Page<DwOrganizationNode> result = svc.getOrganizations(PageRequest.of(0, 1), RowScope.ALL, "ORG1");
        assertThat(result.getTotalElements()).isZero();
    }
}
