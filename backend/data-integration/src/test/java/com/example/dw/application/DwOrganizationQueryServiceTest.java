package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.audit.AuditPort;
import com.example.common.security.RowScope;
import com.example.dw.application.DwOrganizationTreeService.OrganizationTreeSnapshot;
import com.example.dw.application.readmodel.OrganizationReadModelPort;
import com.example.dw.application.readmodel.OrganizationTreeReadModel;
import com.example.dw.domain.HrOrganizationEntity;
import com.example.testing.bdd.Scenario;

@ExtendWith(MockitoExtension.class)
class DwOrganizationQueryServiceTest {

    @Mock
    private DwOrganizationTreeService treeService;
    @Mock
    private OrganizationRowScopeStrategy organizationRowScopeStrategy;
    @Mock
    private OrganizationReadModelPort organizationReadModelPort;
    @Mock
    private AuditPort auditPort;

    private DwOrganizationQueryService service;

    @BeforeEach
    void setUp() {
        service = new DwOrganizationQueryService(treeService, organizationRowScopeStrategy, organizationReadModelPort, auditPort);
    }

    @Test
    void givenAllScope_whenQuery_thenReturnAllFromCache() {
        PageRequest pageable = PageRequest.of(0, 10);
        OrganizationTreeSnapshot snapshot = OrganizationTreeSnapshot.from(List.of(
                sample("ROOT", null),
                sample("CHILD", "ROOT")));
        given(treeService.snapshot()).willReturn(snapshot);

        Scenario.given("전체 가시 범위", () -> service)
                .when("조직 조회", svc -> svc.getOrganizations(pageable, RowScope.ALL, "ROOT"))
                .then("캐시된 트리 활용", page -> assertThat(page.getTotalElements()).isEqualTo(2));
    }

    @Test
    void givenOwnScope_whenQuery_thenReturnSingleNode() {
        PageRequest pageable = PageRequest.of(0, 5);
        OrganizationTreeSnapshot snapshot = OrganizationTreeSnapshot.from(List.of(sample("ORG-B", null)));
        given(treeService.snapshot()).willReturn(snapshot);

        Scenario.given("OWN 스코프", () -> service)
                .when("조직 조회", svc -> svc.getOrganizations(pageable, RowScope.OWN, "ORG-B"))
                .then("단일 조직만 반환", page -> {
                    assertThat(page.getContent()).hasSize(1);
                    assertThat(page.getContent().getFirst().organizationCode()).isEqualTo("ORG-B");
                });
    }

    @Test
    @DisplayName("CUSTOM 스코프에서 전략이 빈 페이지를 반환하면 그대로 전달한다")
    void customScopePropagatesStrategyResult() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<DwOrganizationNode> customPage = new PageImpl<>(List.of(), pageable, 0);
        given(organizationRowScopeStrategy.apply(pageable, "ORG-X")).willReturn(customPage);

        Page<DwOrganizationNode> page = service.getOrganizations(pageable, RowScope.CUSTOM, "ORG-X");

        assertThat(page.getTotalElements()).isZero();
        verify(organizationRowScopeStrategy).apply(pageable, "ORG-X");
    }

    @Test
    @DisplayName("readModel 포트 미주입 + CUSTOM 전략 빈 페이지여도 그대로 반환한다")
    void customScopeWithoutReadModel() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<DwOrganizationNode> customPage = new PageImpl<>(List.of(), pageable, 0);
        given(organizationRowScopeStrategy.apply(pageable, "ORG-Y")).willReturn(customPage);

        DwOrganizationQueryService noReadModel =
                new DwOrganizationQueryService(treeService, organizationRowScopeStrategy, null, auditPort);

        Page<DwOrganizationNode> result = noReadModel.getOrganizations(pageable, RowScope.CUSTOM, "ORG-Y");

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void givenOrgScope_whenQuery_thenIncludeDescendants() {
        PageRequest pageable = PageRequest.of(0, 5);
        OrganizationTreeSnapshot snapshot = OrganizationTreeSnapshot.from(List.of(
                sample("ROOT", null),
                sample("SUB", "ROOT"),
                sample("LEAF", "SUB")));
        given(treeService.snapshot()).willReturn(snapshot);

        Scenario.given("ORG 스코프", () -> service)
                .when("조직 조회", svc -> svc.getOrganizations(pageable, RowScope.ORG, "ROOT"))
                .then("조직 + 하위 조직 포함", page -> assertThat(page.getTotalElements()).isEqualTo(3));
    }

    @Test
    void givenReadModelEnabled_whenQuery_thenUsesRedisSnapshot() {
        PageRequest pageable = PageRequest.of(0, 5);
        List<DwOrganizationNode> nodes = List.of(
                DwOrganizationNode.fromEntity(sample("ROOT", null)),
                DwOrganizationNode.fromEntity(sample("SUB", "ROOT")));
        OrganizationTreeReadModel readModel = new OrganizationTreeReadModel("ver", OffsetDateTime.now(), nodes);
        given(organizationReadModelPort.isEnabled()).willReturn(true);
        given(organizationReadModelPort.load()).willReturn(Optional.of(readModel));

        Scenario.given("Read model 활성화", () -> service)
                .when("조직 조회", svc -> svc.getOrganizations(pageable, RowScope.ALL, "ROOT"))
                .then("Read model 사용", page -> {
                    assertThat(page.getTotalElements()).isEqualTo(2);
                    verify(organizationReadModelPort).load();
                });
    }

    @Test
    void givenCustomScope_whenQuery_thenDelegateToStrategy() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<DwOrganizationNode> customPage = new PageImpl<>(List.of(), pageable, 0);
        given(organizationRowScopeStrategy.apply(pageable, "SUB")).willReturn(customPage);

        Scenario.given("CUSTOM 스코프", () -> service)
                .when("조직 조회", svc -> svc.getOrganizations(pageable, RowScope.CUSTOM, "SUB"))
                .then("커스텀 전략 호출", page -> verify(organizationRowScopeStrategy).apply(pageable, "SUB"));
    }

    @Test
    void givenMissingOrganizationCode_whenQuery_thenThrows() {
        PageRequest pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.getOrganizations(pageable, RowScope.OWN, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNullScope_whenQuery_thenThrows() {
        PageRequest pageable = PageRequest.of(0, 5);

        assertThatThrownBy(() -> service.getOrganizations(pageable, null, "ORG-A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("read model이 비어 있으면 트리 스냅샷으로 폴백한다")
    void fallbackToTreeSnapshotWhenReadModelEmpty() {
        PageRequest pageable = PageRequest.of(0, 5);
        OrganizationTreeSnapshot snapshot = OrganizationTreeSnapshot.from(List.of(sample("ROOT", null)));
        given(organizationReadModelPort.isEnabled()).willReturn(true);
        given(organizationReadModelPort.load()).willReturn(Optional.empty());
        given(treeService.snapshot()).willReturn(snapshot);

        Page<DwOrganizationNode> page = service.getOrganizations(pageable, RowScope.ALL, "ROOT");

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(treeService).snapshot();
    }

    @Test
    @DisplayName("페이지 오프셋이 범위를 벗어나면 빈 페이지를 반환한다")
    void returnsEmptyPageWhenOffsetBeyondSize() {
        PageRequest pageable = PageRequest.of(2, 10); // offset 20
        OrganizationTreeSnapshot snapshot = OrganizationTreeSnapshot.from(List.of(sample("ROOT", null)));
        given(treeService.snapshot()).willReturn(snapshot);

        Page<DwOrganizationNode> page = service.getOrganizations(pageable, RowScope.ALL, "ROOT");

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("read model 포트가 주입되지 않은 경우 트리 스냅샷을 사용한다")
    void usesSnapshotWhenReadModelPortIsNull() {
        PageRequest pageable = PageRequest.of(0, 5);
        DwOrganizationQueryService noReadModelService =
                new DwOrganizationQueryService(treeService, organizationRowScopeStrategy, null, auditPort);
        OrganizationTreeSnapshot snapshot = OrganizationTreeSnapshot.from(List.of(sample("ROOT", null)));
        given(treeService.snapshot()).willReturn(snapshot);

        Page<DwOrganizationNode> page = noReadModelService.getOrganizations(pageable, RowScope.ALL, "ROOT");

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(treeService).snapshot();
    }

    @Test
    @DisplayName("ALL 스코프에서는 organizationCode 없이도 전체를 조회한다")
    void allScopeAllowsNullOrganizationCode() {
        PageRequest pageable = PageRequest.of(0, 5);
        OrganizationTreeSnapshot snapshot = OrganizationTreeSnapshot.from(List.of(sample("ROOT", null)));
        given(treeService.snapshot()).willReturn(snapshot);

        Page<DwOrganizationNode> page = service.getOrganizations(pageable, RowScope.ALL, null);

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("CUSTOM 스코프에서 organizationCode가 없으면 예외를 던진다")
    void customScopeWithNullOrganizationCodeThrows() {
        PageRequest pageable = PageRequest.of(0, 5);

        assertThatThrownBy(() -> service.getOrganizations(pageable, RowScope.CUSTOM, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private HrOrganizationEntity sample(String code, String parent) {
        return HrOrganizationEntity.snapshot(
                code,
                1,
                "Org-" + code,
                parent,
                "ACTIVE",
                LocalDate.of(2020, 1, 1),
                null,
                UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
