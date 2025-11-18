package com.example.hr.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.example.common.security.RowScope;
import com.example.hr.domain.HrOrganizationEntity;
import com.example.hr.infrastructure.persistence.HrOrganizationRepository;
import com.example.testing.bdd.Scenario;

@ExtendWith(MockitoExtension.class)
class HrOrganizationQueryServiceTest {

    @Mock
    private HrOrganizationRepository repository;
    @Mock
    private OrganizationRowScopeStrategy organizationRowScopeStrategy;

    private HrOrganizationQueryService service;

    @BeforeEach
    void setUp() {
        service = new HrOrganizationQueryService(repository, organizationRowScopeStrategy);
    }

    @Test
    void givenAllScope_whenQuery_thenDelegatesToFindAll() {
        PageRequest pageable = PageRequest.of(0, 10);
        given(repository.findAll(pageable)).willReturn(Page.empty(pageable));

        Scenario.given("전체 가시 범위", () -> service)
                .when("조직 조회", svc -> svc.getOrganizations(pageable, RowScope.ALL, "ORG-A"))
                .then("전체 조회 메서드 사용", page -> {
                    verify(repository).findAll(pageable);
                    assertThat(page).isEmpty();
                });
    }

    @Test
    void givenOwnScope_whenQuery_thenFiltersByOrganization() {
        PageRequest pageable = PageRequest.of(0, 5);
        given(repository.findByOrganizationCode("ORG-B", pageable)).willReturn(Page.empty(pageable));

        Scenario.given("OWN 스코프", () -> service)
                .when("조직 조회", svc -> svc.getOrganizations(pageable, RowScope.OWN, "ORG-B"))
                .then("단일 조직 조회", page -> {
                    verify(repository).findByOrganizationCode("ORG-B", pageable);
                    assertThat(page).isEmpty();
                });
    }

    @Test
    void givenOrgScope_whenQuery_thenIncludesChildren() {
        PageRequest pageable = PageRequest.of(0, 5);
        given(repository.findByOrganizationCodeOrParentOrganizationCode("ROOT", "ROOT", pageable))
                .willReturn(Page.empty(pageable));

        Scenario.given("ORG 스코프", () -> service)
                .when("조직 조회", svc -> svc.getOrganizations(pageable, RowScope.ORG, "ROOT"))
                .then("조직 + 하위 조직 포함", page -> verify(repository)
                        .findByOrganizationCodeOrParentOrganizationCode("ROOT", "ROOT", pageable));
    }

    @Test
    void givenCustomScope_whenQuery_thenUsesOrganizationCode() {
        PageRequest pageable = PageRequest.of(0, 5);
        given(organizationRowScopeStrategy.apply(pageable, "SUB")).willReturn(Page.empty(pageable));

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
}
