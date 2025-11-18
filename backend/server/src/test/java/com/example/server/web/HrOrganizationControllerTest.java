package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.hr.application.HrOrganizationQueryService;
import com.example.hr.domain.HrOrganizationEntity;

@ExtendWith(MockitoExtension.class)
class HrOrganizationControllerTest {

    @Mock
    private HrOrganizationQueryService queryService;

    @InjectMocks
    private HrOrganizationController controller;

    @Test
    void givenOrganizations_whenListing_thenReturnPage() {
        HrOrganizationEntity entity = new HrOrganizationEntity();
        entity.setOrganizationCode("ORG001");
        entity.setVersion(1);
        entity.setName("Headquarters");
        entity.setParentOrganizationCode(null);
        entity.setStatus("ACTIVE");
        entity.setEffectiveStart(LocalDate.of(2020, 1, 1));
        entity.setSyncedAt(OffsetDateTime.now(ZoneOffset.UTC));

        PageRequest pageable = PageRequest.of(0, 20);
        given(queryService.getOrganizations(pageable, RowScope.ALL, "ORG001"))
                .willReturn(new PageImpl<>(List.of(entity), pageable, 1));

        AuthContextHolder.set(new AuthContext("tester", "ORG001", "DEFAULT",
                FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL, Map.of()));
        Page<HrOrganizationResponse> response = controller.organizations(0, 20);
        AuthContextHolder.clear();

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).organizationCode()).isEqualTo("ORG001");
        verify(queryService).getOrganizations(pageable, RowScope.ALL, "ORG001");
    }

    @Test
    void givenMissingContext_whenListing_thenThrows() {
        AuthContextHolder.clear();

        assertThatThrownBy(() -> controller.organizations(0, 20))
                .isInstanceOf(AccessDeniedException.class);
    }
}
