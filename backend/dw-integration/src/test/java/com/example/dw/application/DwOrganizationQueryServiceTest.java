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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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

    private DwOrganizationQueryService service;

    @BeforeEach
    void setUp() {
        service = new DwOrganizationQueryService(treeService, organizationRowScopeStrategy, organizationReadModelPort);
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

    private HrOrganizationEntity sample(String code, String parent) {
        HrOrganizationEntity entity = new HrOrganizationEntity();
        entity.setOrganizationCode(code);
        entity.setVersion(1);
        entity.setName("Org-" + code);
        entity.setParentOrganizationCode(parent);
        entity.setStatus("ACTIVE");
        entity.setEffectiveStart(LocalDate.of(2020, 1, 1));
        entity.setEffectiveEnd(null);
        entity.setSourceBatchId(UUID.randomUUID());
        entity.setSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return entity;
    }
}
